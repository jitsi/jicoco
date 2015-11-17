/*
 * LibJitsi-Protocol
 *
 * Copyright @ 2015 Atlassian Pty Ltd
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
 */
package org.jitsi.xmpp.component;

import net.java.sip.communicator.impl.protocol.jabber.extensions.keepalive.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.configuration.*;
import org.jitsi.xmpp.util.*;
import org.jivesoftware.smack.provider.*;
import org.xmpp.component.*;
import org.xmpp.packet.*;

import java.util.*;

/**
 * Base class for XMPP components.
 *
 * @author Pawel Domas
 */
@SuppressWarnings("unused")
public abstract class ComponentBase
    extends AbstractComponent
{
    /**
     * The logger.
     */
    private final static Logger logger = Logger.getLogger(ComponentBase.class);

    /**
     * The name of the property which configures ping interval in ms. -1 to
     * disable pings.
     */
    private final static String PING_INTERVAL_PNAME = "PING_INTERVAL";

    /**
     * The name of the property used to configure ping timeout in ms.
     */
    private final static String PING_TIMEOUT_PNAME = "PING_TIMEOUT";

    /**
     * The name of the property which configures {@link #pingThreshold}.
     */
    private final static String PING_THRESHOLD_PNAME = "PING_THRESHOLD";

    /**
     * The hostname or IP address to which this component will be connected.
     */
    private final String hostname;

    /**
     * The port of XMPP server to which this component will connect.
     */
    private final int port;

    /**
     * The name of main XMPP domain on which this component will be served.
     */
    private final String domain;

    /**
     * The name of subdomain on which this component will be available.
     * So the JID of component will be constructed as follows:<br/>
     * {@link #domain} + ". " + <tt>subdomain</tt><br/>
     * eg. "jicofo" + "." + "example.com" = "jicofo.example.com"
     */
    private final String subdomain;

    /**
     * Password used by the component to authenticate with XMPP server.
     */
    private final String secret;

    /**
     * How often pings will be sent. -1 disables ping checks.
     */
    private long pingInterval = -1;

    /**
     * How long are we going to wait for ping response in ms.
     */
    private long pingTimeout;

    /**
     * FIXME find best value, possibly 1 or 2, but using 3 for the start
     * After how many failures we will consider the connection broken.
     */
    private int pingThreshold;

    /**
     * Map holding ping timeout tasks...
     * FIXME there is similar code in Whack, but no easy way to use it as we can
     * not access {@link org.jivesoftware.whack.ExternalComponent} from here.
     */
    private final Map<String, TimeoutTask> timeouts
        = new HashMap<String, TimeoutTask>();

    /**
     * How many times the ping has failed so far ?
     */
    private int pingFailures = 0;

    /**
     * Time used to schedule ping and timeout tasks.
     */
    private Timer pingTimer;

    /**
     * Indicates if the component has been started.
     *
     * FIXME not 100% sure yet, but probably in order to detect connection
     *       failure it is enough to check only if the component has been
     *       started and do not send pings at all.
     */
    private boolean started;

    /**
     * Default constructor for <tt>ComponentBase</tt>.
     * @param host the hostname or IP address to which this component will be
     *             connected.
     * @param port the port of XMPP server to which this component will connect.
     * @param domain the name of main XMPP domain on which this component will
     *               be served.
     * @param subDomain the name of subdomain on which this component will be
     *                  available.
     * @param secret the password used by the component to authenticate with
     *               XMPP server.
     */
    public ComponentBase(String          host,
                         int             port,
                         String        domain,
                         String     subDomain,
                         String        secret)
    {
        this.hostname = host;
        this.port = port;
        this.domain = domain;
        this.subdomain = subDomain;
        this.secret = secret;
    }

    /**
     * Loads component configuration.
     * @param config <tt>ConfigurationService</tt> instance used to obtain
     *               component's configuration properties.
     * @param configPropertiesBase config properties base string used to
     *        construct full names. To this string dot + config property name
     *        will be added. Example: "org.jitsi.jicofo" + "." + PING_INTERVAL
     */
    protected void loadConfig(ConfigurationService config,
                              String configPropertiesBase)
    {
        configPropertiesBase += ".";

        pingInterval = config.getLong(
            configPropertiesBase + PING_INTERVAL_PNAME, 10000L);

        pingTimeout = config.getLong(
            configPropertiesBase + PING_TIMEOUT_PNAME, 5000L);

        pingThreshold = config.getInt(
            configPropertiesBase + PING_THRESHOLD_PNAME, 3);

        logger.info("Component " + configPropertiesBase  + " config: ");
        logger.info("  ping interval: " + pingInterval + " ms");
        logger.info("  ping timeout: " + pingTimeout + " ms");
        logger.info("  ping threshold: " + pingThreshold);
    }

    /**
     * Method should be called before this component is going to be used for the
     * first time. As contrary to {@link #postComponentStart()} is called every
     * time the component connects to XMPP server.
     */
    public void init()
    {

    }

    /**
     * Call this method in order to release all resources used by this component.
     * After that this instance should be ready for garbage collection.
     */
    public void dispose()
    {

    }

    @Override
    public void postComponentStart()
    {
        super.postComponentStart();

        started = true;

        startPingTask();
    }

    /**
     * Starts ping timer task.
     */
    protected void startPingTask()
    {
        if (pingInterval > 0)
        {
            ProviderManager.getInstance().addIQProvider(
                "ping", "urn:xmpp:ping", new KeepAliveEventProvider());

            pingTimer = new Timer();
            pingTimer.schedule(new PingTask(), pingInterval, pingInterval);
        }
    }

    /**
     * Returns <tt>true</tt> if ping timer task is running.
     */
    protected boolean isPingTaskStarted()
    {
        return pingTimer != null;
    }

    /**
     * Called before component's request queue is cleared.
     */
    @Override
    public void preComponentShutdown()
    {
        started = false;

        super.preComponentShutdown();

        if (pingTimer != null)
        {
            pingTimer.cancel();
            pingTimer = null;
        }
    }

    /**
     * Returns <tt>true</tt> if component's connection to XMPP server is
     * considered alive.
     */
    public boolean isConnectionAlive()
    {
        if (!started)
            return false;

        synchronized (timeouts)
        {
            return pingFailures < pingThreshold;
        }
    }

    @Override
    protected void handleIQResult(IQ iq)
    {
        super.handleIQResult(iq);
        synchronized (timeouts)
        {
            String packetId = iq.getID();
            TimeoutTask timeout = timeouts.get(packetId);
            if (timeout != null)
            {
                timeout.gotResult();
                timeouts.remove(packetId);

                pingFailures = 0;
            }
        }
    }

    /**
     * Returns the hostname or IP address to which this component will be
     * connected.
     */
    public String getHostname()
    {
        return hostname;
    }

    /**
     * Returns the port of XMPP server to which this component will connect.
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Returns the name of main XMPP domain on which this component will be
     * served.
     */
    public String getDomain()
    {
        return domain;
    }

    /**
     * Returns the name of subdomain on which this component will be available.
     */
    public String getSubdomain()
    {
        return subdomain;
    }

    /**
     * Returns the password used by the component to authenticate with XMPP
     * server.
     */
    public String getSecret()
    {
        return secret;
    }

    /**
     * Tasks sends ping to the server and schedules timeout task.
     */
    private class PingTask extends TimerTask
    {
        @Override
        public void run()
        {
            try
            {
                String domain = getDomain();

                // domain = domain.substring(domain.indexOf(".") + 1);

                KeepAliveEvent ping = new KeepAliveEvent(null, domain);

                IQ pingIq = IQUtils.convert(ping);

                logger.debug("Sending ping IQ: " + ping.toXML());

                send(pingIq);

                synchronized (timeouts)
                {
                    String packetId = pingIq.getID();
                    TimeoutTask timeout = new TimeoutTask(packetId);

                    timeouts.put(packetId, timeout);

                    pingTimer.schedule(timeout, pingTimeout);
                }
            }
            catch (Exception e)
            {
                logger.error("Failed to send ping", e);

                pingFailures++;
            }
        }
    }

    /**
     * FIXME: make generic so that components can use it to track timeouts
     * like it's done in {@link org.jivesoftware.whack.ExternalComponent}.
     *
     * Timeout task for ping packets.
     */
    private class TimeoutTask extends TimerTask
    {
        private final String packetId;

        private boolean hasResult;

        public TimeoutTask(String packetId)
        {
            this.packetId = packetId;
        }

        public void gotResult()
        {
            this.hasResult = true;

            logger.debug("Got ping response for: " + packetId);
        }

        @Override
        public void run()
        {
            if (!hasResult)
            {
                pingFailures++;

                logger.error("Ping timeout for ID: " + packetId);

                synchronized (timeouts)
                {
                    timeouts.remove(packetId);
                }
            }
        }
    }
}
