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

import org.jetbrains.annotations.*;
import org.jitsi.utils.concurrent.*;
import org.jitsi.utils.logging2.*;
import org.jitsi.retry.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.iqrequest.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.tcp.*;
import org.jivesoftware.smack.util.*;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.muc.*;
import org.jivesoftware.smackx.muc.packet.*;
import org.jivesoftware.smackx.ping.*;
import org.jivesoftware.smackx.xdata.form.*;
import org.jxmpp.jid.*;
import org.jxmpp.jid.impl.*;
import org.jxmpp.jid.parts.*;
import org.jitsi.xmpp.*;
import org.jxmpp.stringprep.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import static org.jitsi.xmpp.util.ErrorUtilKt.createError;

/**
 * The {@link MucClient} is responsible for handling a single XMPP connection
 * on which a single MUC is joined.
 *
 * @author bbaldino
 * @author Boris Grozev
 */
public class MucClient
{
    private static final int DEFAULT_PING_INTERVAL_SECONDS = 30;

    static
    {
        XMPPTCPConnection.setUseStreamManagementDefault(false);
        XMPPTCPConnection.setUseStreamManagementResumptionDefault(false);
        PingManager.setDefaultPingInterval(DEFAULT_PING_INTERVAL_SECONDS);
    }

    /**
     * The {@link Logger} used by the {@link MucClient} class and its instances
     * for logging output.
     */
    private static final Logger classLogger = new LoggerImpl(MucClient.class.getName());

    /**
     * The IQ types we are interested in.
     */
    private static final IQ.Type[] IQ_TYPES = new IQ.Type[]{ IQ.Type.get, IQ.Type.set};

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
            classLogger.error("Failed to parse domain: " + domain, xse);
            return null;
        }

        XMPPTCPConnectionConfiguration.Builder builder
            = XMPPTCPConnectionConfiguration.builder()
                .setHost(config.getHostname())
                .setXmppDomain(domainJid)
                .setUsernameAndPassword(config.getUsername(), config.getPassword());

        String portStr = config.getPort();

        if (portStr != null && !portStr.isEmpty())
        {
            builder.setPort(Integer.parseInt(portStr));
        }

        if (config.getDisableCertificateVerification())
        {
            classLogger.warn("Disabling certificate verification!");
            builder.setCustomX509TrustManager(new TrustAllX509TrustManager());
            builder.setHostnameVerifier(new TrustAllHostnameVerifier());
        }

        ConnectionConfiguration.SecurityMode securityMode = config.getSecurityMode();
        if (securityMode == null)
        {
            String hostname = config.getHostname();
            /* We want to allow security to be disabled on loopback. */
            if (hostname.equals("localhost") || hostname.equals("127.0.0.1") || hostname.equals("::1"))
            {
                securityMode = ConnectionConfiguration.SecurityMode.ifpossible;
            }
            else
            {
                securityMode = ConnectionConfiguration.SecurityMode.required;
            }
        }
        if (securityMode == ConnectionConfiguration.SecurityMode.disabled)
        {
            classLogger.warn("XMPP security is disabled!");
        }

        builder.setSecurityMode(securityMode);

        // Uses SASL Mechanisms ANONYMOUS and PLAIN to authenticate, but tries to authenticate with GSSAPI when
        // it's offered by the server which does not work with the server components using jicoco.
        // Disable GSSAPI.
        SASLAuthentication.unregisterSASLMechanism("org.jivesoftware.smack.sasl.javax.SASLGSSAPIMechanism");

        return builder.build();
    }

    /**
     * The {@link AbstractXMPPConnection} object for the connection to
     * the xmpp server
     */
    private AbstractXMPPConnection xmppConnection;

    /**
     * The connect loop: we keep this running forever and it re-establishes the
     * connection in case it's broken.
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
    @NotNull
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
     * The ping fail listener.
     */
    private final PingFailedListener pingFailedListener = new PingFailedListenerImpl();

    /**
     * The reconnection listener.
     */
    private final ReconnectionListener reconnectionListener = new ReconnectionListener()
    {
        @Override
        public void reconnectingIn(int i)
        {
            if (i == 0)
            {
                mucClientManager.reconnecting(MucClient.this);
            }
            logger.info("Reconnecting in " + i);
        }

        @Override
        public void reconnectionFailed(Exception e)
        {
            mucClientManager.reconnectionFailed(MucClient.this);
            logger.warn("Reconnection failed: ", e);
        }
    };

    /**
     * Creates and XMPP connection for the given {@code config}, connects, and
     * joins the MUC described by the {@code config}.
     *
     * @param config xmpp connection details
     */
    MucClient(@NotNull MucClientConfiguration config, MucClientManager mucClientManager)
    {
        this.mucClientManager = mucClientManager;
        // TODO: use the simpler Map.of() when updated to java 11+
        logger = classLogger.createChildLogger(
                MucClient.class.getName(),
                Stream.of(new String[][] {
                    { "id", config.getId() },
                    { "hostname", config.getHostname() }
                }).collect(Collectors.toMap(data -> data[0], data -> data[1])));
        this.config = config;
    }

    @NotNull
    public MucClientConfiguration getConfig()
    {
        return config;
    }

    /**
     * Initializes the executor and starts initializing, connecting and logging
     * in of this muc client.
     */
    void start()
    {
        this.executor = ExecutorUtils.newScheduledThreadPool(1, true, MucClientManager.class.getSimpleName());
        this.connectRetry = new RetryStrategy(this.executor);
        this.executor.execute(() ->
        {
            try
            {
                this.initializeConnectAndJoin();
            }
            catch(Exception e)
            {
                logger.error("Failed to initialize and start a MucClient: ", e);
            }
        });
    }

    /**
     * Initializes this instance (by extracting the necessary fields from its
     * configuration), connects and logs into the XMPP server, and joins all
     * MUCs that the configuration describes.
     */
    private void initializeConnectAndJoin()
        throws Exception
    {
        logger.info("Initializing a new MucClient for " + config);

        if (!config.isComplete())
        {
            throw new IllegalArgumentException("incomplete configuration");
        }

        mucNickname = Resourcepart.from(config.getMucNickname());
        if ("sync".equalsIgnoreCase(config.getIqHandlerMode()))
        {
            iqHandlerMode = IQRequestHandler.Mode.sync;
        }

        xmppConnection = new XMPPTCPConnection(createXMPPTCPConnectionConfiguration(config));
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(xmppConnection);
        PingManager pingManager = PingManager.getInstanceFor(xmppConnection);
        if (pingManager != null)
        {
            pingManager.registerPingFailedListener(pingFailedListener);
        }

        // Register the disco#info features.
        mucClientManager.getFeatures().forEach(sdm::addFeature);

        ReconnectionManager reconnectionManager = ReconnectionManager.getInstanceFor(xmppConnection);
        reconnectionManager.disableAutomaticReconnection();

        xmppConnection.addConnectionListener(new ConnectionListener()
        {
            @Override
            public void connected(XMPPConnection xmppConnection)
            {
                mucClientManager.connected(MucClient.this);
                logger.info("Connected.");
            }

            @Override
            public void authenticated(XMPPConnection xmppConnection, boolean b)
            {
                logger.info("Authenticated, b=" + b);
            }

            @Override
            public void connectionClosed()
            {
                mucClientManager.closed(MucClient.this);
                logger.info("Closed.");
            }

            @Override
            public void connectionClosedOnError(Exception e)
            {
                mucClientManager.closedOnError(MucClient.this);
                logger.warn("Closed on error:", e);
            }
        });

        ReconnectionManager.getInstanceFor(xmppConnection).addReconnectionListener(reconnectionListener);

        mucClientManager.getRegisteredIqs().forEach(this::registerIQ);
        setIQListener(mucClientManager.getIqListener());

        logger.info("Dispatching a thread to connect and login.");
        this.connectRetry.runRetryingTask(new SimpleRetryTask(0, 5000, true, getConnectAndLoginCallable()));
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
        for (String mucJidStr : config.getMucJids())
        {
            EntityBareJid mucJid = JidCreate.entityBareFrom(mucJidStr);
            MucWrapper mucWrapper = getOrCreateMucState(mucJid);
            mucWrapper.join(mucJid);
        }
    }

    /**
     * Whether the XMPP connection is currently connected (and authenticated).
     */
    boolean isConnected()
    {
        return xmppConnection != null && xmppConnection.isConnected() && xmppConnection.isAuthenticated();
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
            logger.warn("Failed to send stanza: ", e);
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
        if (!isConnected())
        {
            logger.warn("Cannot set presence extension: not connected.");
            return;
        }
        mucs.values().forEach(ms -> ms.setPresenceExtensions(extensions));
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
     * @param iq the IQ which represents the IQ type (i.e. an element name and a namespace).
     * @param requireResponse whether to send an error stanza as a response if the {@link IQListener} produces
     * {@code null} for requests of this type.
     */
    void registerIQ(IQ iq, boolean requireResponse)
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
                        logger.debug(() -> "Received an IQ with type " + type + ": " + iqRequest.toString());
                        return handleIq(iqRequest, requireResponse);
                    }
                }
            );
        }
    }

    /**
     * Handles an IQ received from Smack by passing it to the listener which is
     * registered.
     * @param iq the IQ to handle.
     * @param requireResponse whether to send an error stanza as a response if the {@link IQListener} produces
     * {@code null}.
     * @return the response.
     */
    private IQ handleIq(IQ iq, boolean requireResponse)
    {
        IQ responseIq = null;

        EntityBareJid fromJid = iq.getFrom().asEntityBareJidIfPossible();
        String fromJidStr = fromJid.toString().toLowerCase();
        if (this.config.getMucJids().stream()
                .noneMatch(mucJid -> mucJid.toLowerCase().equals(fromJidStr)))
        {
            logger.warn("Received an IQ from a non-MUC member: " + fromJid);
            return createError(iq, StanzaError.Condition.forbidden);
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
                logger.warn("Exception processing IQ, returning internal server error. Request: " + iq, e);
                responseIq = createError(iq, StanzaError.Condition.internal_server_error, e.getMessage());
            }
        }

        if (requireResponse && responseIq == null)
        {
            logger.info(
                    "Failed to produce a response for IQ, returning internal server error. Request: " + iq);
            responseIq = createError(iq, StanzaError.Condition.internal_server_error, "Unknown error");
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
        this.connectRetry.cancel();

        ReconnectionManager.getInstanceFor(xmppConnection).removeReconnectionListener(reconnectionListener);

        if (this.executor != null)
        {
            this.executor.shutdown();
            this.executor = null;
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

        PingManager pingManager = PingManager.getInstanceFor(xmppConnection);
        if (pingManager != null)
        {
            pingManager.unregisterPingFailedListener(pingFailedListener);
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
                logger.warn("Error connecting:", t);
                return true;
            }

            if (!xmppConnection.isAuthenticated())
            {
                logger.info("Logging in.");
                try
                {
                    xmppConnection.login();
                }
                catch (SmackException.AlreadyLoggedInException e)
                {
                    logger.info("Already logged in.");
                }
                catch (Exception e)
                {
                    // We've observed the XMPPTCPConnection get in a broken state where it is connected, but unable to
                    // login (because the locally cached SASL mechanisms supported by the server are empty). We
                    // disconnect in order to trigger a re-connect and clear that state on the next attempt.
                    logger.warn("Failed to login. Disconnecting to trigger a re-connect.", e);
                    xmppConnection.disconnect(null);
                    return true;
                }

                try
                {
                    joinMucs();
                }
                catch(Exception e)
                {
                    logger.warn("Failed to join the MUCs.", e);
                    return true;
                }
            }

            return true;
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
        private PresenceBuilder lastPresenceSent;

        /**
         * Intercepts presence packets sent by smack and saves the last one.
         */
        private final Consumer<PresenceBuilder> presenceInterceptor = presence ->
        {
            // The initial presence sent by smack contains an empty "x"
            // extension. If this extension is included in a subsequent stanza,
            // it indicates that the client lost its synchronization and causes
            // the MUC service to re-send the presence of each occupant in the
            // room.
            // Make a copy to make sure we don't remove the extension from the actual initial presence.
            PresenceBuilder nextLastPresence = presence.build().asBuilder((String) null)
                .removeExtension(MUCInitialPresence.ELEMENT, MUCInitialPresence.NAMESPACE);
            synchronized (this)
            {
                lastPresenceSent = nextLastPresence;
            }
        };

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
                leave();
            }
            MultiUserChatManager mucManager = MultiUserChatManager.getInstanceFor(xmppConnection);
            muc = mucManager.getMultiUserChat(mucJid);
            muc.addPresenceInterceptor(presenceInterceptor);

            MultiUserChat.MucCreateConfigFormHandle mucCreateHandle = muc.createOrJoin(mucNickname);
            if (mucCreateHandle != null)
            {
                // the room was just created. Let's send a config
                // making the room non-anonymous, so that others can
                // recognize our JID
                Form config = muc.getConfigurationForm();
                FillableForm answer = config.getFillableForm();
                // Room non-anonymous
                String whoisFieldName = "muc#roomconfig_whois";
                answer.setAnswer(whoisFieldName, "anyone");
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
            Presence updatedPresence;
            synchronized (this)
            {
                if (lastPresenceSent == null)
                {
                    logger.warn("Cannot set presence extensions: no previous presence available.");
                    return;
                }

                // Remove the old extensions if present and override
                extensions.forEach(lastPresenceSent::overrideExtension);
                updatedPresence = lastPresenceSent.build();
            }

            try
            {
                xmppConnection.sendStanza(updatedPresence);
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
            Presence updatedPresence = null;
            synchronized (this)
            {
                if (lastPresenceSent == null)
                {
                    return;
                }

                if (lastPresenceSent.removeExtension(elementName, namespace) != null)
                {
                    updatedPresence = lastPresenceSent.build();
                }
            }

            if (updatedPresence != null)
            {
                try
                {
                    xmppConnection.sendStanza(updatedPresence);
                }
                catch (Exception e)
                {
                    logger.error("Failed to send stanza:", e);
                }
            }
        }

        /**
         * Resets the field which stores the last presence Smack sent on our behalf.
         */
        private synchronized void resetLastPresenceSent()
        {
            logger.debug("Resetting lastPresenceSent");
            lastPresenceSent = null;
        }
    }

    /**
     * Handle ping failures from {@link PingManager}.
     */
    private class PingFailedListenerImpl
        implements PingFailedListener
    {
        /**
         * Handle a ping failure: disconnect to trigger a re-connect if the XMPP connection still thinks that it is
         * connected.
         */
        @Override
        public void pingFailed()
        {
            logger.warn("Ping failed, the XMPP connection needs to reconnect.");
            mucClientManager.pingFailed(MucClient.this);

            if (xmppConnection.isConnected() && xmppConnection.isAuthenticated())
            {
                logger.warn("XMPP connection still connected, will trigger a disconnect.");
                // two pings in a row fail and the XMPP connection is connected and authenticated.
                // This is a weird situation that we have seen in the past when using VPN.
                // Everything stays like this forever as the socket remains open on the OS level
                // and it is never dropped. We will trigger reconnect just in case.
                try
                {
                    xmppConnection.disconnect(null);
                }
                catch (Exception e)
                {
                    logger.warn("Exception while disconnecting");
                }
            }
        }
    }
}
