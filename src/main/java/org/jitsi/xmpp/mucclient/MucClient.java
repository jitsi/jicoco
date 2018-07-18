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

import org.jitsi.util.*;
import org.jitsi.xmpp.util.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.iqrequest.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.tcp.*;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.ping.*;
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
    /**
     * The {@link Logger} used by the {@link MucClient} class and its instances
     * for logging output.
     */
    private static final Logger logger
        =  org.jitsi.util.Logger.getLogger(MucClient.class);

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
            logger.error("Failed to parse domain: " + domain);
            return null;
        }

        XMPPTCPConnectionConfiguration.Builder builder
            = XMPPTCPConnectionConfiguration.builder()
                .setHost(config.getHostname())
                .setXmppDomain(domainJid)
                .setUsernameAndPassword(
                    config.getUsername(),
                    config.getPassword());

        if (config.getDisableCertificateVerification())
        {
            logger.warn("Disabling certificate verification!");
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
     * Creates and XMPP connection for the given {@code config}, connects, and
     * joins the MUC described by the {@code config}.
     *
     * @param config xmpp connection details
     */
    MucClient(MucClientConfiguration config, MucClientManager mucClientManager)
    {
        this.mucClientManager = mucClientManager;
        this.config = config;
    }

    /**
     * Initializes this instance (by extracting the necessary fields from its
     * configuration), connects and logs into the XMPP server, and joins all
     * MUCs that the configuration describes.
     * @throws Exception
     */
    void initializeConnectAndJoin()
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
        if ("sync".equals(config.getIqHandlerMode()))
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
                logger.info(MucClient.this + " connected");
            }

            @Override
            public void authenticated(XMPPConnection xmppConnection, boolean b)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " authenticated, b=" + b);
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
                logger.info(MucClient.this + " closed");
            }

            @Override
            public void connectionClosedOnError(Exception e)
            {
                logger.info(MucClient.this + " closed on error:", e);
            }

            @Override
            public void reconnectionSuccessful()
            {
                logger.info(MucClient.this + " reconnection successful");

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
                    logger.debug(MucClient.this + " reconnecting in " + i);
                }
            }

            @Override
            public void reconnectionFailed(Exception e)
            {
                logger.info(MucClient.this + " reconnection failed");
            }
        });

        mucClientManager.getRegisteredIqs().forEach(this::registerIQ);
        setIQListener(mucClientManager.getIqListener());

        // Note: the connected() and authenticated() callbacks execute
        // synchronously, so this will also trigger the call to joinMucs()
        if (logger.isDebugEnabled())
        {
            logger.debug(this + " about to connect and login.");
        }
        xmppConnection.connect().login();
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
            logger.debug(this + " about to join MUCs.");
        }

        for (String mucJidStr : config.getMucJids())
        {
            EntityBareJid mucJid = JidCreate.entityBareFrom(mucJidStr);
            MucWrapper mucWrapper = getOrCreateMucState(mucJid);
            mucWrapper.join(mucJid);
        }
    }

    /**
     * Gets the {@link MucWrapper} instance for a particular JID, creating it
     * if necessary.
     * @param mucJid the MUC JID.
     * @return the {@link MucWrapper} instance.
     */
    private MucWrapper getOrCreateMucState(Jid mucJid)
    {
        synchronized (mucs)
        {
            MucWrapper mucWrapper = mucs.get(mucJid);
            if (mucWrapper == null)
            {
                mucWrapper = new MucWrapper();
                mucs.put(mucJid, mucWrapper);
            }

            return mucWrapper;
        }
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

        IQListener iqListener = this.iqListener;
        if (iqListener == null)
        {
            logger.error("Received an IQ, but the listener is null.");
        }
        else
        {
            responseIq = iqListener.handleIq(iq, this);
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
        private PresenceListener presenceInterceptor = this::presenceSent;

        /**
         * Notifies this instance that Smack sent presence in the MUC on our behalf.
         * @param presence the presence which was sent.
         */
        private void presenceSent(Presence presence)
        {
            lastPresenceSent = presence;
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
            lastPresenceSent = null;

            if (muc != null)
            {
                muc.removePresenceInterceptor(presenceInterceptor);
                logger.info("Leaving a MUC we already occupy.");
                muc.leave();
            }
            MultiUserChatManager mucManager
                = MultiUserChatManager.getInstanceFor(xmppConnection);
            muc = mucManager.getMultiUserChat(mucJid);
            if (presenceInterceptor != null)
            {
                muc.addPresenceInterceptor(presenceInterceptor);
            }
            muc.createOrJoin(mucNickname);
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

            // Remove the old extensions if present
            extensions.forEach(
                extension -> lastPresenceSent.removeExtension(
                    extension.getElementName(), extension.getNamespace()));

            extensions.forEach(lastPresenceSent::addExtension);

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
