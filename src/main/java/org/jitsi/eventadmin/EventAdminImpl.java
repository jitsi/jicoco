/*
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
package org.jitsi.eventadmin;

import org.jitsi.util.*;

import org.osgi.framework.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * The implementation of <tt>EventAdmin</tt> interface. It does implement
 * {@link ServiceListener} interface in order to cache <tt>EventHandler</tt>
 * instances. It has basic support for topic filtering by being able to process
 * "*" wildcard.
 *
 * @author Pawel Domas
 */
public class EventAdminImpl
    implements EventAdmin,
               ServiceListener
{
    /**
     * The logger.
     */
    static private final Logger logger = Logger.getLogger(EventAdminImpl.class);

    /**
     * The OSGi bundle context this instance is running on.
     */
    private BundleContext bundleContext;

    /**
     * Event callback executor for async events.
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Maps service reference to <tt>HandlerRef</tt> for convenience in
     * add/remove operations.
     */
    private Map<ServiceReference<EventHandler>, HandlerRef> handlers
        = new HashMap<>();

    /**
     * The field is used to cache the current list of all <tt>HandlerRef</tt> to
     * iterate over when firing an event. It is updated on every modification
     * to {@link #handlers} map.
     */
    private List<HandlerRef> handlerRefListCache = new ArrayList<>();

    /**
     * Creates {@link Pattern} that is supposed to match all the topics
     * described by <tt>topic</tt> array. It does combine all the topics with
     * 'or' regular expression. Also wildcard sign '*' is converted to Java
     * regular expression equivalent '.*'.
     *
     * @param topics the array of event topics to be converted into
     *               <tt>Pattern</tt>.
     *
     * @return <tt>Pattern</tt> that will match all the topics from
     *         <tt>topic</tt> array.
     */
    static private Pattern createTopicsPattern(String[] topics)
    {
        StringBuilder combined = new StringBuilder();
        for (int i=0; i<topics.length; i++)
        {
            String topic = topics[i].replace("*", ".*");

            combined.append("(").append(topic).append(")");

            if (i < topics.length - 1)
            {
                combined.append("|");
            }
        }

        try
        {
            String patternStr = combined.toString();

            logger.debug(
                "Created: " + patternStr + " from: " + Arrays.toString(topics));

            return Pattern.compile(patternStr);
        }
        catch (PatternSyntaxException exc)
        {
            logger.error("Failed to parse: " + combined);
            return null;
        }
    }

    public void start(BundleContext ctx)
        throws InvalidSyntaxException
    {
        ctx.addServiceListener(
            this, "(objectclass=" + EventHandler.class.getName() + ")");

        this.bundleContext = ctx;
    }

    public void stop(BundleContext ctx)
    {
        ctx.removeServiceListener(this);
    }

    /**
     * Checks if <tt>EventHandler</tt> referenced in given <tt>HandlerRef</tt>
     * is subscribed to given <tt>topic</tt>.
     *
     * @param handlerRef <tt>HandlerRef</tt> instance that refers to
     *                   <tt>EventHandler</tt> which we're going to check.
     * @param topic the event topics we're checking for.
     *
     * @return <tt>true</tt> if event handler is subscribed for given topic.
     */
    private boolean hasTopic(HandlerRef handlerRef, String topic)
    {
        return handlerRef.topicsPattern.matcher(topic).matches();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postEvent(Event event)
    {
        eventImpl(event, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendEvent(final Event event)
    {
        eventImpl(event, true);
    }

    private void eventImpl(final Event event, boolean synchronous)
    {
        String eventTopic = event.getTopic();

        if (StringUtils.isNullOrEmpty(eventTopic))
        {
            logger.warn(
                "Some trash event without correct topic: " + eventTopic);
            return;
        }

        List<HandlerRef> handlerRefs = getCurrentHandlerList();
        for (final HandlerRef handlerRef : handlerRefs)
        {
            if (hasTopic(handlerRef, eventTopic))
            {
                if (synchronous)
                {
                    callEventHandler(handlerRef, event);
                }
                else
                {
                    executor.submit(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            callEventHandler(handlerRef, event);
                        }
                    });
                }
            }
        }
    }

    private void callEventHandler(final HandlerRef handlerRef,
                                  final Event           event)
    {
        final EventHandler handler = handlerRef.handler;
        if (handler == null)
        {
            // EventHandler has been unregistered
            return;
        }

        try
        {
            handler.handleEvent(event);
        }
        catch (Exception e)
        {
            logger.error("EventHandler exception", e);
        }
    }

    /**
     * <tt>EventHandler</tt> being packed into <tt>HandlerRef</tt> with topic
     * array verification.
     *
     * @param handlerRef the reference to <tt>EventHandler</tt> service for
     *                   which we're going to create new <tt>HandlerRef</tt>.
     *
     * @return <tt>HandlerRef</tt> instance for given <tt>EventHandler</tt>
     *         service. <tt>null</tt> will be returned if either topics array
     *         is not valid or if we were unable to access the service through
     *         the reference.
     */
    private HandlerRef newHandlerRef(ServiceReference<EventHandler> handlerRef)
    {
        Object topicsObj
            = handlerRef.getProperty(EventConstants.EVENT_TOPIC);

        if (!(topicsObj instanceof String[]))
            return null;

        String[] topics = (String[]) topicsObj;
        if (topics.length == 0)
            return null;

        EventHandler handler = bundleContext.getService(handlerRef);
        if (handler == null)
            return null;

        Pattern pattern = createTopicsPattern(topics);
        if (pattern == null)
            return null;

        return new HandlerRef(handler, pattern);
    }

    /**
     * This method will return current list of all <tt>HandlerRef</tt>. The
     * cache reference is updated whenever the new handler is added/removed.
     *
     * @return the current list of all <tt>HandlerRef</tt>.
     */
    private List<HandlerRef> getCurrentHandlerList()
    {
        return handlerRefListCache;
    }

    /**
     * Listens for new <tt>EventHandler</tt> service instance registrations.
     * {@inheritDoc}
     */
    // Class should be enforced by service listener filter
    @SuppressWarnings("unchecked")
    @Override
    synchronized public void serviceChanged(ServiceEvent serviceEvent)
    {
        ServiceReference<EventHandler> svcHandlerRef
            = (ServiceReference<EventHandler>)
                    serviceEvent.getServiceReference();

        switch (serviceEvent.getType())
        {
            case ServiceEvent.REGISTERED:
                HandlerRef newHandlerRef = newHandlerRef(svcHandlerRef);
                if (newHandlerRef != null)
                {
                    handlers.put(svcHandlerRef, newHandlerRef);
                    // Update cached reference
                    handlerRefListCache = new ArrayList<>(handlers.values());
                }
                break;
            case ServiceEvent.UNREGISTERING:
                HandlerRef ref = handlers.remove(svcHandlerRef);
                if (ref != null)
                {
                    // Update cached reference
                    handlerRefListCache = new ArrayList<>(handlers.values());
                    // Cancel execution if service has been unregistered, but
                    // the async task has been scheduled already
                    ref.handler = null;
                }
                break;
        }
    }

    /**
     * Structure holding <tt>EventHandler</tt> and it's topics.
     */
    private class HandlerRef
    {
        EventHandler handler;

        Pattern topicsPattern;

        HandlerRef(EventHandler handler, Pattern topicsRegExpr)
        {
            this.handler = handler;
            this.topicsPattern = topicsRegExpr;
        }
    }
}
