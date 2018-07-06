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
import java.util.function.*;

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
    private static final org.jitsi.util.Logger logger
        =  org.jitsi.util.Logger.getLogger(MucClient.class);

    /**
     * Creates a Smack {@link XMPPTCPConnectionConfiguration} based on
     * a {@link MucClientManager.Configuration}.
     * @param config the {@link MucClientManager.Configuration} which describes
     * the connection.
     * @return the {@link XMPPTCPConnectionConfiguration}.
     */
    private static XMPPTCPConnectionConfiguration
        createXMPPTCPConnectionConfiguration(
            MucClientManager.Configuration config)
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
    private final AbstractXMPPConnection xmppConnection;

    /**
     * The {@link MultiUserChat} object for the MUC we'll be joining.
     */
    private MultiUserChat muc;

    /**
     * The {@link MucClientManager} which owns this {@link MucClient}.
     */
    private final MucClientManager mucClientManager;

    /**
     * The listener, if any, to call when we receive an IQ from Smack.
     */
    private Function<IQ, IQ> iqListener;

    /**
     * Stores our last MUC presence packet for future update.
     */
    private Presence lastPresenceSent;

    /**
     * Intercepts presence packets sent by smack and saves the last one.
     */
    private PresenceListener presenceInterceptor = this::presenceSent;


    /**
     * The JID of the MUC this client is to join.
     */
    private EntityBareJid mucJid;

    /**
     * The nickname of this client in the MUC.
     */
    private Resourcepart mucNickname;

    /**
     * The mode to use with Smack {@link IQRequestHandler}s.
     */
    private IQRequestHandler.Mode iqHandlerMode = IQRequestHandler.Mode.async;

    /**
     * Creates and XMPP connection for the given {@code config}, connects, and
     * joins the MUC described by the {@code config}.
     *
     * @param config xmpp connection details
     * @throws Exception from {@link XMPPTCPConnection#connect()} or
     * {@link XMPPTCPConnection#login()}
     *
     * TODO: specific exceptions
     */
    MucClient(MucClientManager.Configuration config, MucClientManager mucClientManager)
        throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Initializing a new MucClient for " + config);
        }

        this.mucClientManager = mucClientManager;

        mucJid = JidCreate.entityBareFrom(config.getMuc());
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
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " connected");
                }
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
                    createOrJoinMuc();
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void connectionClosed()
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " closed");
                }
            }

            @Override
            public void connectionClosedOnError(Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " closed on error:", e);
                }
            }

            @Override
            public void reconnectionSuccessful()
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " reconnection successful");
                }

                try
                {
                    createOrJoinMuc();
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void reconnectingIn(int i)
            {
                lastPresenceSent = null;
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " reconnecting in " + i);
                }
            }

            @Override
            public void reconnectionFailed(Exception e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug(MucClient.this + " reconnection failed");
                }
            }
        });

        mucClientManager.getRegisteredIqs().forEach(this::registerIQ);
        setIQListener(mucClientManager.getIqListener());

        // Note: the connected() and authenticated() callbacks execute
        // synchronously, so this will also trigger the call to
        // createOrJoinMuc()
        xmppConnection.connect().login();
    }

    /**
     * Create and/or join the muc named mucJid with the given nickname
     * @param mucJid the jid of the muc to join
     * @param nickname the nickname to use when joining the muc
     * @throws Exception if creating or joining the MUC fails.
     */
    private void createOrJoinMuc()
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
            logger.warn("Leaving a MUC we already occupy.");
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

        addExtensions();
    }

    /**
     * Adds the presence extensions of the {@link MucClientManager} to our
     * presence in the MUC.
     */
    private void addExtensions()
    {
        setPresenceExtensions(mucClientManager.getPresenceExtensions());
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
        return "[MucClient host=" + xmppConnection.getHost() + "]";
    }

    /**
     * Notifies this instance that Smack sent presence in the MUC on our behalf.
     * @param presence the presence which was sent.
     */
    private void presenceSent(Presence presence)
    {
        lastPresenceSent = presence;
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
        if (lastPresenceSent == null)
        {
            logger.info("Not setting an extension yet, presence not sent.");
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
     * Removes from our presence any extensions with the given
     * {@code elementName} and {@code namespace}.
     * @param elementName the element name to match.
     * @param namespace the namespace to match.
     */
    public void removePresenceExtension(String elementName, String namespace)
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
     * Sets the listener for IQs.
     * @param iqListener the listener to set.
     */
    void setIQListener(Function<IQ, IQ> iqListener)
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
        for (IQ.Type type : new IQ.Type[]{ IQ.Type.get, IQ.Type.set })
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

        Function<IQ, IQ> iqListener = this.iqListener;
        if (iqListener == null)
        {
            logger.error("Received an IQ, but the listener is null.");
        }
        else
        {
            responseIq = iqListener.apply(iq);
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
}
