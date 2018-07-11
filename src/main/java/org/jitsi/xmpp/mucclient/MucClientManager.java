/*
 * Copyright @ 2018 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.jitsi.xmpp.mucclient;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.*;
import org.jxmpp.util.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Manages a set of {@link MucClient}, each of which represents an XMPP client
 * connection (via an {@link XMPPConnection}) and a Multi-User Chat. Allows
 * loading the configuration of a {@link MucClient} from properties in a
 * {@link ConfigurationService}, as well as adding and removing
 * {@link MucClient}s dynamically.
 *
 * @author Boris Grozev
 */
public class MucClientManager
{
    /**
     * The {@link Logger} used by the {@link MucClientManager} class and its
     * instances for logging output.
     */
    private static final Logger logger
        =  org.jitsi.util.Logger.getLogger(MucClientManager.class);

    /**
     * Loads a list of {@link MucClientConfiguration} objects based on
     * properties read from a {@link ConfigurationService} with a given
     * {@code prefix}.
     *
     * The configurations can be described with properties like this with an
     * ID of "":
     * PREFIX.HOSTNAME=hostname1
     * PREFIX.DOMAIN=domain
     *
     * Or like this with an ID of "id":
     * PREFIX.id1.HOSTNAME=hostname2
     * PREFIX.id1.USERNAME=user
     *
     * @param config the {@link ConfigurationService} to read properties from.
     * @param prefix the prefix for property names.
     *
     * @return a list of {@link MucClientConfiguration}s described by properties in
     * {@code config} with a prefix of {@code prefix}.
     */
    private static Collection<MucClientConfiguration> loadConfig(
        ConfigurationService config, String prefix)
    {
        Map<String, MucClientConfiguration> configurations = new HashMap<>();

        for (String pname : config.getPropertyNamesByPrefix(prefix, false))
        {
            String stripped = pname.substring(prefix.length());
            String id = "";
            String prop = stripped;
            if (stripped.contains("."))
            {
                id = stripped.substring(0, stripped.indexOf("."));
                prop = stripped.substring(id.length() + 1);
            }

            MucClientConfiguration c
                = configurations.computeIfAbsent(
                id,
                MucClientConfiguration::new);
            c.setProperty(prop, config.getString(pname));
        }

        configurations.values().removeIf(c ->
             {
                 if (!c.isComplete())
                 {
                     logger.warn(
                         "Ignoring incomplete configuration with id=" + c.getId());
                     return true;
                 }
                 return false;
             });

        return configurations.values();
    }

    /**
     * Maps a hostname to the {@link MucClient} associated with it.
     */
    private final Map<String, MucClient> mucClients
        = new ConcurrentHashMap<>();

    /**
     * Contains the list of features to use for disco#info.
     */
    private final Set<String> features = new HashSet<>();

    /**
     * The listener which is to be called when any of our {@link MucClient}s
     * receive an IQ from Smack.
     */
    private IQListener iqListener;

    /**
     * The list of IQs which {@link #iqListener} is interested in
     * receiving, represented by {@link IQ} instances.
     */
    private List<IQ> registeredIqs = new LinkedList<>();

    /**
     * The list of extensions to be added to the presence in the MUC in each
     * of our {@link MucClient}s.
     */
    private final Map<String, ExtensionElement> presenceExtensions
        = new ConcurrentHashMap<>();

    /**
     * An object used to synchronize access to some of the fields in this
     * instance (whichever were deemed to need it).
     */
    private final Object syncRoot = new Object();

    private final Executor executor;

    /**
     * Initializes a new {@link MucClientManager} instance.
     *
     */
    public MucClientManager()
    {
        this(new String[0]);
    }

    /**
     * Initializes a new {@link MucClientManager} instance.
     *
     * @param features the features to use for disco#info.
     */
    public MucClientManager(String[] features)
    {
        this(features,
             ExecutorUtils.newCachedThreadPool(
                 true,
                 MucClientManager.class.getSimpleName()));
    }

    /**
     * Initializes a new {@link MucClientManager} instance.
     *
     * @param features the features to use for disco#info.
     */
    public MucClientManager(String[] features, Executor executor)
    {
        SmackConfiguration.setUnknownIqRequestReplyMode(
            SmackConfiguration.UnknownIqRequestReplyMode
                .replyFeatureNotImplemented);

        this.executor = executor;
        if (features != null)
        {
            this.features.addAll(Arrays.asList(features));
        }
    }

    /**
     * Adds described with properties in a {@link ConfigurationService} with
     * a specific {@code prefix.}
     * @param cfg the {@link ConfigurationService} which contains properties
     * describing the clients to add.
     * @param prefix the prefix of properties describing the clients to add.
     */
    public void addMucClients(ConfigurationService cfg, String prefix)
    {
        Collection<MucClientConfiguration> configurations = loadConfig(cfg, prefix);

        configurations.forEach(this::addMucClient);
    }

    /**
     * Asynchronously adds a new {@link MucClient} with a specific
     * {@link MucClientConfiguration}.
     * @param config the configuration of the new {@link MucClient}.
     */
    public void addMucClient(MucClientConfiguration config)
    {
        executor.execute(() -> this.doAddMucClient(config));
    }

    /**
     * Creates and starts new {@link MucClient}. This method blocks until
     * the XMPP connection is established and the MUC is joined. If these things
     * happen successfully, the new client is added to {@link #mucClients}.
     *
     * @param config the configuration for the new client.
     */
    private void doAddMucClient(MucClientConfiguration config)
    {
        try
        {
            MucClient mucClient;
            synchronized (syncRoot)
            {
                mucClient = mucClients.get(config.getId());
                if (mucClient != null)
                {
                    logger.error("Not adding a new MUC client, ID already exists.");
                    return;
                }

                mucClient = new MucClient(config, MucClientManager.this);
                mucClients.put(config.getId(), mucClient);
            }

            mucClient.initializeConnectAndJoin();
        }
        catch (Exception e)
        {
            logger.error("Failed to create and start a MucClient: ", e);
        }
    }

    /**
     * Adds an {@link ExtensionElement} to the presence of all our
     * {@link MucClient}s, and removes any other extensions with the same
     * element name and namespace, if any exist.
     *
     * @param extension the extension to add.
     */
    public void setPresenceExtension(ExtensionElement extension)
    {
        synchronized (syncRoot)
        {
            saveExtension(extension);

            mucClients.values().forEach(
                mucClient -> mucClient.setPresenceExtension(extension));
        }
    }

    /**
     * Saves an extension element in the local map, replacing any previous
     * extension with the same element name and namespace.
     * @param extension the extension to save.
     */
    private void saveExtension(ExtensionElement extension)
    {
        synchronized (syncRoot)
        {
            ExtensionElement previousExtension
                = presenceExtensions.put(
                    XmppStringUtils.generateKey(
                        extension.getElementName(), extension.getNamespace()),
                    extension);

            if (previousExtension != null && logger.isDebugEnabled())
            {
                logger.debug(
                    "Replacing presence extension: " + previousExtension);
            }
        }
    }

    /**
     * Removes the extension with a given element name and namespace from the
     * local map.
     * @param elementName the element name of the extension to remove.
     * @param namespace the namespace of the extension to remove.
     */
    private void removeExtension(String elementName, String namespace)
    {
        presenceExtensions
            .remove(XmppStringUtils.generateKey(elementName, namespace));
    }

    /**
     * @return the presence extensions as a list.
     */
    Collection<ExtensionElement> getPresenceExtensions()
    {
        return presenceExtensions.values();
    }

    /**
     * Removes an {@link ExtensionElement} with a particular element name and
     * namespace from the presence of all our {@link MucClient}s.
     *
     * @param elementName the name of the element of the extension to remove.
     * @param namespace the namespace of the element of the extension to remove.
     */
    public void removePresenceExtension(String elementName, String namespace)
    {
        synchronized (syncRoot)
        {
            removeExtension(elementName, namespace);

            mucClients.values().forEach(
                mucClient -> mucClient.removePresenceExtension(
                    elementName, namespace));
        }
    }

    /**
     * @return the set of features to use in disco#info.
     */
    Set<String> getFeatures()
    {
        return features;
    }

    /**
     * Sets the IQ listener which will be called when an IQ of a registered
     * type is received (see {@link #registerIQ(IQ)}).
     * @param iqListener the listener to set.
     */
    public void setIQListener(IQListener iqListener)
    {
        synchronized (syncRoot)
        {
            // Save it, so it is accessible to MucClients added later
            if (this.iqListener != null)
            {
                logger.info("Replacing an existing IQ listener.");
            }
            this.iqListener = iqListener;

            mucClients.values().forEach(m -> m.setIQListener(iqListener));
        }
    }

    /**
     * Indicates to this instance that the registered IQ listener is
     * interested in IQs with a specific child element name and namespace,
     * represented in an {@link IQ}.
     *
     * @param iq the IQ which represents a [element name, namespace] pair.
     */
    public void registerIQ(IQ iq)
    {
        synchronized (syncRoot)
        {
            registeredIqs.add(iq);
            mucClients.values()
                .forEach(mucClient -> mucClient.registerIQ(iq));
        }
    }

    /**
     * Indicates to this instance that the registered IQ listener is
     * interested in IQs with a specific element name and namespace.
     *
     * @param elementName the child element name.
     * @param namespace the child element namespace.
     */
    public void registerIQ(String elementName, String namespace)
    {
        registerIQ(new IQ(elementName, namespace)
            {
                @Override
                protected IQChildElementXmlStringBuilder getIQChildElementBuilder(
                    IQChildElementXmlStringBuilder xml)
                {
                    return null;
                }
            });
    }

    /**
     * @return the list of registered IQ types.
     */
    List<IQ> getRegisteredIqs()
    {
        return new LinkedList<>(registeredIqs);
    }

    /**
     * @return the IQ listener.
     */
    IQListener getIqListener()
    {
        return iqListener;
    }

}
