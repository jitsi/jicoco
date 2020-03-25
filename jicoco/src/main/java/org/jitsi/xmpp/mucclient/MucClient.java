/*
 * Copyright @ 2018 - present 8x8, Inc.
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

import org.jitsi.utils.collections.*;
import org.jitsi.utils.concurrent.*;
import org.jitsi.utils.logging2.*;
import org.jitsi.retry.*;
import org.jitsi.xmpp.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.iqrequest.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.id.*;
import org.jivesoftware.smack.tcp.*;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.muc.packet.*;
import org.jivesoftware.smackx.ping.*;
import org.jivesoftware.smackx.xdata.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.jid.parts.*;
import org.jitsi.xmpp.*;
import org.jxmpp.stringprep.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * The {@link MucClient} is responsible for handling a single XMPP connection
 * on which a single MUC is joined.
 *
 * @author bbaldino
 * @author Boris Grozev
 */
public class MucClient
{
    static
    {
        XMPPTCPConnection.setUseStreamManagementDefault(false);
        XMPPTCPConnection.setUseStreamManagementResumptionDefault(false);
    }

    /**
     * The {@link Logger} used by the {@link MucClient} class and its instances
     * for logging output.
     */
    private static final Logger classLogger
            = new LoggerImpl(MucClient.class.getName());

    /**
     * The IQ types we are interested in.
     */
    private static final IQ.Type[] IQ_TYPES
        = new IQ.Type[]{ IQ.Type.get, IQ.Type.set};

    /**
     * Creates a Smack {@link XMPPTCPConnectionConfiguration} based on
     * a {@link MucClientConfiguration}.
     * @param config the {@link MucClientConfiguration} which describes
     * the connection.
     * @return the {@link XMPPTCPConnectionConfiguration}.
     */
    private static XMPPTCPConnectionConfiguration
        createXMPPTCPConnectionConfiguration(
            MucClientConfiguration config)
    {
        String domain = config.getDomain();
        if (domain == null)
        {
            domain = config.getHostname();
        }

        DomainBareJid domainJid;
        try
        {
            domainJid = JidCreate.domainBareFrom(domain);
        }
        catch (XmppStringprepException xse)
        {
            classLogger.error("Failed to parse domain: " + domain);
            return null;
        }

        XMPPTCPConnectionConfiguration.Builder builder
            = XMPPTCPConnectionConfiguration.builder()
                .setHost(config.getHostname())
                .setXmppDomain(domainJid)
                .setUsernameAndPassword(
                    config.getUsername(),
                    config.getPassword());

        String portStr = config.getPort();

        if (portStr != null && !portStr.isEmpty()) {
            builder.setPort(Integer.parseInt(portStr));
        }

        if (config.getDisableCertificateVerification())
        {
            classLogger.warn("Disabling certificate verification!");
            builder.setCustomX509TrustManager(new TrustAllX509TrustManager());
            builder.setHostnameVerifier(new TrustAllHostnameVerifier());
        }

        return builder.build();
    }

    static
    {
        PingManager.setDefaultPingInterval(30);
    }

    /**
     * The {@link AbstractXMPPConnection} object for the connection to
     * the xmpp server
     */
    private AbstractXMPPConnection xmppConnection;

    /**
     * The retry we do on initial connect. After xmpp is connected the Smack
     * reconnect kick-ins.
     */
    private RetryStrategy connectRetry;

    /**
     * The executor to execute connect, retry connection and login.
     */
    private ScheduledExecutorService executor;

    /**
     * The {@link MucClientManager} which owns this {@link MucClient}.
     */
    private final MucClientManager mucClientManager;

    /**
     * The listener, if any, to call when we receive an IQ from Smack.
     */
    private IQListener iqListener;

    /**
     * The nickname of this client in the MUC.
     */
    private Resourcepart mucNickname;

    /**
     * The mode to use with Smack {@link IQRequestHandler}s.
     */
    private IQRequestHandler.Mode iqHandlerMode = IQRequestHandler.Mode.async;

    /**
     * This {@link MucClient}'s configuration.
     */
    private final MucClientConfiguration config;

    /**
     * Contains the smack {@link MultiUserChat} objects that this
     * {@link MucClient} maintains (mapped by their MUC JIDs).
     */
    private final Map<Jid, MucWrapper> mucs = new ConcurrentHashMap<>();

    /**
     * The {@link Logger} used by the {@link MucClient} class and its instances
     * for logging output.
     */
    private final Logger logger;


    /**
     * Creates and XMPP connection for the given {@code config}, connects, and
     * joins the MUC described by the {@code config}.
     *
     * @param config xmpp connection details
     */
    MucClient(MucClientConfiguration config, MucClientManager mucClientManager)
    {
        this.mucClientManager = mucClientManager;
        logger = classLogger.createChildLogger(
                MucClient.class.getName(),
                JMap.of(
                    "id", config.getId(),
                    "hostname", config.getHostname()));
        this.config = config;
    }

    /**
     * Initializes the executor and starts initializing, connecting and logging
     * in of this muc client.
     */
    void start()
    {
        this.executor = ExecutorUtils.newScheduledThreadPool(
            1, true, MucClientManager.class.getSimpleName());

        this.executor.execute(() ->
        {
            try
            {
                this.initializeConnectAndJoin();
            }
            catch(Exception e)
            {
                logger.error(
                    "Failed to initialize and start a MucClient: ", e);
            }
        });
    }

    /**
     * Initializes this instance (by extracting the necessary fields from its
     * configuration), connects and logs into the XMPP server, and joins all
     * MUCs that the configuration describes.
     * @throws Exception
     */
    private void initializeConnectAndJoin()
        throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Initializing a new MucClient for " + config);
        }

        if (!config.isComplete())
        {
            throw new IllegalArgumentException("incomplete configuration");
        }

        mucNickname = Resourcepart.from(config.getMucNickname());
        if ("sync".equalsIgnoreCase(config.getIqHandlerMode()))
        {
            iqHandlerMode = IQRequestHandler.Mode.sync;
        }

        xmppConnection
            = new XMPPTCPConnection(
                createXMPPTCPConnectionConfiguration(config));
        ServiceDiscoveryManager sdm
            = ServiceDiscoveryManager.getInstanceFor(xmppConnection);

        // Register the disco#info features.
        mucClientManager.getFeatures().forEach(sdm::addFeature);

        ReconnectionManager reconnectionManager
            = ReconnectionManager.getInstanceFor(xmppConnection);
        reconnectionManager.enableAutomaticReconnection();

        xmppConnection.addConnectionListener(new ConnectionListener()
        {
            @Override
            public void connected(XMPPConnection xmppConnection)
            {
                logger.info("Connected.");
            }

            @Override
            public void authenticated(XMPPConnection xmppConnection, boolean b)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Authenticated, b=" + b);
                }
                try
                {
                    joinMucs();
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void connectionClosed()
            {
                logger.info("Closed.");
            }

            @Override
            public void connectionClosedOnError(Exception e)
            {
                logger.warn("Closed on error:", e);
            }

            @Override
            public void reconnectionSuccessful()
            {
                logger.info("Reconnection successful.");

                try
                {
                    joinMucs();
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void reconnectingIn(int i)
            {
                mucs.values().forEach(MucWrapper::resetLastPresenceSent);
                if (logger.isDebugEnabled())
                {
                    logger.debug("Reconnecting in " + i);
                }
            }

            @Override
            public void reconnectionFailed(Exception e)
            {
                logger.warn("Reconnection failed: ", e);
            }
        });

        mucClientManager.getRegisteredIqs().forEach(this::registerIQ);
        setIQListener(mucClientManager.getIqListener());

        // Note: the connected() and authenticated() callbacks execute
        // synchronously, so this will also trigger the call to joinMucs()
        if (logger.isDebugEnabled())
        {
            logger.debug("About to connect and login.");
        }

        this.connectRetry = new RetryStrategy(this.executor);

        this.connectRetry.runRetryingTask(new SimpleRetryTask(
            0, 5000, true, getConnectAndLoginCallable()));
    }

    /**
     * Create and/or join the MUCs described in the configuration.
     */
    private void joinMucs()
        throws SmackException.NotConnectedException,
               SmackException.NoResponseException,
               InterruptedException,
               XMPPException.XMPPErrorException,
               MultiUserChatException.MucAlreadyJoinedException,
               MultiUserChatException.NotAMucServiceException,
               XmppStringprepException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("About to join MUCs: " + config.getMucJids());
        }

        for (String mucJidStr : config.getMucJids())
        {
            EntityBareJid mucJid = JidCreate.entityBareFrom(mucJidStr);
            MucWrapper mucWrapper = getOrCreateMucState(mucJid);
            mucWrapper.join(mucJid);
        }
    }

    /**
     * Whether the XMPP connection is currently connected.
     */
    boolean isConnected()
    {
        return xmppConnection != null && xmppConnection.isConnected();
    }

    /**
     * The number of MUCs configured for this {@link MucClient}.
     */
    int getMucsCount()
    {
        return config.getMucJids().size();
    }

    /**
     * The number of MUCs that have been joined.
     * @return
     */
    int getMucsJoinedCount()
    {
        if (!isConnected())
        {
            return 0;
        }

        return (int) mucs.values().stream()
            .filter(mucWrapper -> mucWrapper.muc.isJoined())
            .count();

    }

    /**
     * Gets the {@link MucWrapper} instance for a particular JID, creating it
     * if necessary.
     * @param mucJid the MUC JID.
     * @return the {@link MucWrapper} instance.
     */
    private MucWrapper getOrCreateMucState(Jid mucJid)
    {
        return mucs.computeIfAbsent(mucJid, (k) -> new MucWrapper());
    }

    /**
     * Send an xmpp stanza on the xmpp connection
     * @param stanza the stanza to send
     * @return true if it is sent successfully, false otherwise
     */
    public boolean sendStanza(Stanza stanza)
    {
        try
        {
            xmppConnection.sendStanza(stanza);
            return true;
        }
        catch (Exception e)
        {
            logger.warn("Failed to send stanza: " + e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[MucClient id=" + config.getId()
            + " hostname=" + config.getHostname() + "]";
    }

    /**
     * Adds an extension to our presence in the MUC. If our presence already
     * contains an extension with the same namespace and element name, the old
     * one is removed.
     * @param extension the extension to add.
     */
    public void setPresenceExtension(ExtensionElement extension)
    {
        setPresenceExtensions(Collections.singletonList(extension));
    }

    /**
     * Adds a set of extension to our presence in the MUC. If our presence
     * already contains extensions that match the namespace or element of any
     * of the extensions to be added, the old ones are removed.
     * @param extensions the extensions to add.
     */
    public void setPresenceExtensions(Collection<ExtensionElement> extensions)
    {
        mucs.values().forEach(ms->ms.setPresenceExtensions(extensions));
    }

    /**
     * Removes from our presence any extensions with the given
     * {@code elementName} and {@code namespace}.
     * @param elementName the element name to match.
     * @param namespace the namespace to match.
     */
    public void removePresenceExtension(String elementName, String namespace)
    {
        mucs.values().forEach(ms->ms.removePresenceExtension(elementName, namespace));
    }

    /**
     * Sets the listener for IQs.
     * @param iqListener the listener to set.
     */
    void setIQListener(IQListener iqListener)
    {
        this.iqListener = iqListener;
    }

    /**
     * Indicates to this instance that the {@link #iqListener} is interested
     * in IQs of a specific type, represented as an {@link IQ} instance.
     * @param iq the IQ which represents the IQ type (i.e. an element name and
     * a namespace).
     */
    void registerIQ(IQ iq)
    {
        for (IQ.Type type : IQ_TYPES)
        {
            xmppConnection.registerIQRequestHandler(
                new AbstractIqRequestHandler(iq.getChildElementName(),
                                             iq.getChildElementNamespace(),
                                             type,
                                             iqHandlerMode)
                {
                    @Override
                    public IQ handleIQRequest(IQ iqRequest)
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug(
                                "Received an IQ with type " + type + ": "
                                    + iqRequest.toString());
                        }
                        return handleIq(iqRequest);
                    }
                }
            );
        }
    }

    /**
     * Handles an IQ received from Smack by passing it to the listener which is
     * registered.
     * @param iq the IQ to handle.
     * @return the response.
     */
    private IQ handleIq(IQ iq)
    {
        IQ responseIq = null;

        EntityBareJid fromJid = iq.getFrom().asEntityBareJidIfPossible();
        String fromJidStr = fromJid.toString().toLowerCase();
        if (fromJid == null
            || !this.config.getMucJids().stream().anyMatch(
                    mucJid -> mucJid.toLowerCase().equals(fromJidStr)))
        {
            logger.warn("Received an IQ from a non-MUC member: " + fromJid);
            return IQUtils.createError(iq, XMPPError.Condition.forbidden);
        }

        IQListener iqListener = this.iqListener;
        if (iqListener == null)
        {
            logger.error("Received an IQ, but the listener is null.");
        }
        else
        {
            try
            {
                responseIq = iqListener.handleIq(iq, this);
            }
            catch (Exception e)
            {
                logger.warn(
                    "Exception processing IQ, returning internal" +
                        " server error. Request: " + iq.toString(), e);

                responseIq
                    = IQUtils.createError(
                    iq,
                    XMPPError.Condition.internal_server_error,
                    e.getMessage());
            }
        }

        if (responseIq == null)
        {
            logger.info(
                "Failed to produce a response for IQ, returning internal" +
                    " server error. Request: " +iq.toString());

            responseIq
                = IQUtils.createError(
                    iq,
                    XMPPError.Condition.internal_server_error,
                    "Unknown error");
        }

        return responseIq;
    }

    /**
     * @return  the ID of this {@link MucClient}.
     */
    public String getId()
    {
        return config.getId();
    }

    /**
     * Leaves all MUCs and disconnects from the XMPP server.
     */
    void stop()
    {
        if (this.connectRetry != null)
        {
            this.connectRetry.cancel();
        }

        if (this.executor != null)
        {
            this.executor.shutdown();
        }

        // If we are still not connected leave and disconnect my through
        // errors
        try
        {
            mucs.values().forEach(MucWrapper::leave);
        }
        catch(Exception e)
        {
            logger.error("Error leaving mucs", e);
        }

        try
        {
            xmppConnection.disconnect();
        }
        catch(Exception e)
        {
            logger.error("Error disconnecting xmpp connection", e);
        }
    }

    /**
     * The callable returned by this method describes the task of connecting
     * and login to XMPP service.
     *
     * @return <tt>Callable<Boolean></tt> which returns <tt>true</tt> as long
     *         as we're failing to connect.
     */
    private Callable<Boolean> getConnectAndLoginCallable()
    {
        return () ->
        {
            try
            {
                if (!xmppConnection.isConnected())
                {
                    xmppConnection.connect();
                }
            }
            catch(Exception t)
            {
                logger.warn(MucClient.this + " error connecting", t);

                return true;
            }

            logger.info("Logging in.");
            xmppConnection.login();

            executor.shutdown();

            return false;
        };
    }

    /**
     * Wraps a {@link MultiUserChat} with logic for adding extensions to our
     * own presence.
     */
    private class MucWrapper
    {
        /**
         * The {@link MultiUserChat} object for the MUC we'll be joining.
         */
        private MultiUserChat muc;

        /**
         * Stores our last MUC presence packet for future update.
         */
        private Presence lastPresenceSent;

        /**
         * Intercepts presence packets sent by smack and saves the last one.
         */
        private final PresenceListener presenceInterceptor = this::presenceSent;

        /**
         * Notifies this instance that Smack sent presence in the MUC on our behalf.
         * @param presence the presence which was sent.
         */
        private void presenceSent(Presence presence)
        {
            lastPresenceSent = presence;
        }

        /**
         * Leaves the MUC.
         */
        private void leave()
        {
            try
            {
                muc.leave();
            }
            catch (Exception e)
            {
                logger.warn("Error while trying to leave a MUC: ", e);
            }

            muc = null;
        }

        /**
         * Joins the MUC.
         * @param mucJid the JID of the MUC to join.
         */
        private void join(EntityBareJid mucJid)
            throws SmackException.NotConnectedException,
                   SmackException.NoResponseException,
                   InterruptedException,
                   XMPPException.XMPPErrorException,
                   MultiUserChatException.MucAlreadyJoinedException,
                   MultiUserChatException.NotAMucServiceException

        {
            // We're about to join or re-join the MUC.
            resetLastPresenceSent();

            if (muc != null)
            {
                muc.removePresenceInterceptor(presenceInterceptor);
                logger.info("Leaving a MUC we already occupy.");
                muc.leave();
            }
            MultiUserChatManager mucManager
                = MultiUserChatManager.getInstanceFor(xmppConnection);
            muc = mucManager.getMultiUserChat(mucJid);
            muc.addPresenceInterceptor(presenceInterceptor);

            MultiUserChat.MucCreateConfigFormHandle mucCreateHandle
                = muc.createOrJoin(mucNickname);
            if (mucCreateHandle != null)
            {
                // the room was just created. Let's send a config
                // making the room non-anonymous, so that others can
                // recognize our JID
                Form config = muc.getConfigurationForm();
                Form answer = config.createAnswerForm();
                // Room non-anonymous
                String whoisFieldName = "muc#roomconfig_whois";
                FormField whois = answer.getField(whoisFieldName);
                if (whois == null)
                {
                    whois = new FormField(whoisFieldName);
                    answer.addField(whois);
                }
                whois.addValue("anyone");
                muc.sendConfigurationForm(answer);
            }
            logger.info("Joined MUC: " + mucJid);

            setPresenceExtensions(mucClientManager.getPresenceExtensions());
        }

        /**
         * Adds a set of extensions to our presence in this MUC.
         * @param extensions the extensions to add.
         */
        void setPresenceExtensions(Collection<ExtensionElement> extensions)
        {
            if (lastPresenceSent == null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.info(
                        "Not setting an extension yet, presence not sent.");
                }
                return;
            }

            // The initial presence sent by smack contains an empty "x"
            // extension. If this extension is included in a subsequent stanza,
            // it indicates that the client lost its synchronization and causes
            // the MUC service to re-send the presence of each occupant in the
            // room.
            lastPresenceSent.removeExtension(
                MUCInitialPresence.ELEMENT, MUCInitialPresence.NAMESPACE);

            // Remove the old extensions if present and override
            extensions.forEach(lastPresenceSent::overrideExtension);

            lastPresenceSent.setStanzaId(StanzaIdUtil.newStanzaId());

            try
            {
                xmppConnection.sendStanza(lastPresenceSent);
            }
            catch (Exception e)
            {
                logger.error("Failed to send stanza:", e);
            }
        }

        /**
         * Removes from our presence in the MUC any extensions with the given
         * {@code elementName} and {@code namespace}.
         * @param elementName the element name to match.
         * @param namespace the namespace to match.
         */
        private void removePresenceExtension(String elementName, String namespace)
        {
            if (lastPresenceSent != null
                && lastPresenceSent.removeExtension(elementName, namespace) != null)
            {
                try
                {
                    xmppConnection.sendStanza(lastPresenceSent);
                }
                catch (Exception e)
                {
                    logger.error("Failed to send stanza:", e);
                }
            }
        }

        /**
         * Resets the field which stores the last presence Smack sent on our
         * behalf.
         */
        private void resetLastPresenceSent()
        {
            lastPresenceSent = null;
        }
    }
}
