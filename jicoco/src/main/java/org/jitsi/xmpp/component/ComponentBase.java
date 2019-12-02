/*
 * Copyright @ 2015 - present, 8x8 Inc
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

import org.jitsi.service.configuration.*;
import org.jitsi.utils.logging.*;
import org.jitsi.xmpp.util.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smackx.ping.packet.*;
import org.jivesoftware.smackx.ping.provider.*;
import org.jxmpp.jid.impl.*;
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
     * The default value of 100 ms for {@link #processingTimeLimit}.
     */
    public final static long DEFAULT_PROCESSING_TIME_LIMIT = 100;

    /**
     * The name of the property which configures ping interval in ms. -1 to
     * disable pings.
     */
    public final static String PING_INTERVAL_PNAME = "PING_INTERVAL";

    /**
     * The name of the property used to configure ping timeout in ms.
     */
    public final static String PING_TIMEOUT_PNAME = "PING_TIMEOUT";

    /**
     * The name of the property which configures {@link #pingThreshold}.
     */
    public final static String PING_THRESHOLD_PNAME = "PING_THRESHOLD";

    /**
     * The name of the property which configures {@link #processingTimeLimit}.
     */
    public final static String PROCESSING_TIME_LIMIT_PNAME
        = "PROCESSING_TIME_LIMIT";

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
     * The time limit for processing IQs by the component implementation which
     * will be used to log error messages for easier detection/debugging
     * of the eventual issues.
     */
    private long processingTimeLimit;

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
        super(17 /* executor pool size */, 1000 /* packet queue size */, true);

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

        processingTimeLimit = config.getLong(
            configPropertiesBase + PROCESSING_TIME_LIMIT_PNAME,
            DEFAULT_PROCESSING_TIME_LIMIT);

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
        synchronized (timeouts)
        {
            if (pingInterval > 0)
            {
                ProviderManager.addIQProvider(
                    "ping", "urn:xmpp:ping", new PingProvider());

                pingTimer = new Timer();
                pingTimer.schedule(new PingTask(), pingInterval, pingInterval);
            }
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

        synchronized (timeouts)
        {
            if (pingTimer != null)
            {
                pingTimer.cancel();
                pingTimer = null;
            }

            timeouts.clear();
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

    /**
     * {@inheritDoc}
     *
     * Should *NEVER* be called by the subclass directly.
     *
     * The method is final in order to enforce measurement of the IQ processing
     * time. Subclasses should implement the processing in
     * {@link #handleIQGetImpl(IQ)}. The semantics of that method remain
     * the same as of {@link AbstractComponent#handleIQGet(IQ)}.
     */
    @Override
    final protected IQ handleIQGet(IQ iq)
        throws Exception
    {
        final long startTimestamp = System.currentTimeMillis();
        try
        {
            return handleIQGetImpl(iq);
        }
        finally
        {
            verifyProcessingTime(startTimestamp, iq);
        }
    }

    /**
     * Handles an <tt>IQ</tt> stanza of type <tt>get</tt>.
     *
     * @param iq the <tt>IQ</tt> stanza of type <tt>get</tt> which represents
     * the request to handle.
     *
     * @return an <tt>IQ</tt> stanza which represents the response to
     * the specified request or <tt>null</tt> to reply with
     * <tt>feature-not-implemented</tt>.
     *
     * @throws Exception to reply with <tt>internal-server-error</tt> to the
     * specified request.
     *
     * @see AbstractComponent#handleIQGet(IQ)
     */
    protected IQ handleIQGetImpl(IQ iq) throws Exception
    {
        return super.handleIQGet(iq);
    }

    /**
     * {@inheritDoc}
     *
     * Should *NEVER* be called by the subclass directly.
     *
     * The method is final in order to enforce measurement of the IQ processing
     * time. Subclasses should implement the processing in
     * {@link #handleIQSetImpl(IQ)}. The semantics of that method remain
     * the same as of {@link AbstractComponent#handleIQSet(IQ)}.
     */
    @Override
    final protected IQ handleIQSet(IQ iq)
        throws Exception
    {
        final long startTimestamp = System.currentTimeMillis();
        try
        {
            return handleIQSetImpl(iq);
        }
        finally
        {
            verifyProcessingTime(startTimestamp, iq);
        }
    }

    /**
     * Handles an <tt>IQ</tt> stanza of type <tt>set</tt>.
     *
     * @param iq the <tt>IQ</tt> stanza of type <tt>set</tt> which represents
     * the request to handle.
     *
     * @return an <tt>IQ</tt> stanza which represents the response to
     * the specified request or <tt>null</tt> to reply with
     * <tt>feature-not-implemented</tt>.
     *
     * @throws Exception to reply with <tt>internal-server-error</tt> to the
     * specified request.
     *
     * @see AbstractComponent#handleIQSet(IQ)
     */
    protected IQ handleIQSetImpl(IQ iq) throws Exception
    {
        return super.handleIQSet(iq);
    }

    /**
     * {@inheritDoc}
     *
     *  Should *NEVER* be called by the subclass directly.
     *
     * The method is final in order to enforce measurement of the IQ processing
     * time. Subclasses should implement the processing in
     * {@link #handleIQResultImpl(IQ)}. The semantics of that method remain
     * the same as of {@link AbstractComponent#handleIQResult(IQ)}.
     */
    @Override
    final protected void handleIQResult(IQ iq)
    {
        final long startTimestamp = System.currentTimeMillis();
        try
        {
            handleIQResultImpl(iq);
        }
        finally
        {
            verifyProcessingTime(startTimestamp, iq);
        }
    }

    /**
     * Handles an <tt>IQ</tt> stanza of type <tt>result</tt>.
     *
     * @param iq the <tt>IQ</tt> stanza of type <tt>result</tt> which represents
     * the response to one of the IQs previously sent by this component
     * instance.
     *
     * @see AbstractComponent#handleIQResult(IQ)
     */
    protected void handleIQResultImpl(IQ iq)
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
     * {@inheritDoc}
     *
     * Should *NEVER* be called by the subclass directly.
     *
     * The method is final in order to enforce measurement of the IQ processing
     * time. Subclasses should implement the processing in
     * {@link #handleIQErrorImpl(IQ)}. The semantics of that method remain
     * the same as of {@link AbstractComponent#handleIQError(IQ)}.
     */
    @Override
    final protected void handleIQError(IQ iq)
    {
        final long startTimestamp = System.currentTimeMillis();
        try
        {
            handleIQErrorImpl(iq);
        }
        finally
        {
            verifyProcessingTime(startTimestamp, iq);
        }
    }

    /**
     * Handles an <tt>IQ</tt> stanza of type <tt>error</tt>.
     *
     * @param iq the <tt>IQ</tt> stanza of type <tt>error</tt> which represents
     * an error response to one of the IQs previously sent by this component
     * instance.
     *
     * @see AbstractComponent#handleIQError(IQ)
     */
    protected void handleIQErrorImpl(IQ iq)
    {
        super.handleIQError(iq);
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
     * Utility method for measuring and logging an error if the recommended
     * processing time limit has been exceeded (configurable with
     * {@link #PROCESSING_TIME_LIMIT_PNAME}).
     *
     * NOTE The best way for implementing this timer would be in
     * {@link AbstractComponent#processQueuedPacket(Packet)}, but the method
     * is final.
     *
     * @param startTimestamp the timestamp obtained using
     * {@link System#currentTimeMillis()} when the IQ processing has started
     * @param iq the <tt>IQ</tt> for which the processing time will be validated
     */
    private void verifyProcessingTime(long startTimestamp, IQ iq)
    {
        long processingTime = System.currentTimeMillis() - startTimestamp;
        if (processingTime > processingTimeLimit)
        {
            logger.warn(
                    "PROCESSING TIME LIMIT EXCEEDED - it took "
                        + processingTime + "ms to process: " + iq.toXML());
        }
        else if (logger.isDebugEnabled())
        {
            logger.debug(
                    "It took " + processingTime  + "ms to process packet: "
                        + iq.getID());
        }
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
                synchronized (timeouts)
                {
                    if (pingTimer == null)
                        return; // cancelled

                    String domain = getDomain();
                    String subdomain = getSubdomain();
                    Ping ping = new Ping(JidCreate.domainBareFrom(domain));
                    ping.setFrom(JidCreate.domainBareFrom(
                            subdomain + "." + domain));

                    IQ pingIq = IQUtils.convert(ping);

                    logger.debug("Sending ping IQ: " + ping.toXML());

                    send(pingIq);

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
            synchronized (timeouts)
            {
                this.hasResult = true;

                logger.debug("Got ping response for: " + packetId);
            }
        }

        @Override
        public void run()
        {
            synchronized (timeouts)
            {
                if (!hasResult && timeouts.containsKey(packetId))
                {
                    pingFailures++;

                    logger.error("Ping timeout for ID: " + packetId);

                    timeouts.remove(packetId);
                }
            }
        }
    }
}
