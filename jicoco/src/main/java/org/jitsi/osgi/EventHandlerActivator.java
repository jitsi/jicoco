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
package org.jitsi.osgi;

import org.jitsi.eventadmin.*;

import org.osgi.framework.*;

/**
 * Base class for <tt>BundleActivator</tt>s that needs to subscribe as
 * an <tt>EventHandler</tt>.
 *
 * @author Pawel Domas
 */
public abstract class EventHandlerActivator
    implements BundleActivator,
               EventHandler
{
    /**
     * The array of <tt>Event</tt> topics to which this <tt>EventHandler</tt>
     * instance is subscribed to.
     */
    private final String[] eventTopics;

    /**
     * <tt>EventHandler</tt> service registration.
     */
    private ServiceRegistration<EventHandler> eventHandlerRegistration;

    /**
     * Creates new instance of <tt>EventHandlerActivator</tt>.
     * @param topics the array of <tt>String</tt> which contains the topics for
     *               the {@link Event} to which this <tt>EventHandler</tt> will
     *               be subscribed to.
     */
    public EventHandlerActivator(String[] topics)
    {
        this.eventTopics = topics;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(BundleContext bundleContext)
        throws Exception
    {
        eventHandlerRegistration
            = EventUtil.registerEventHandler(
                bundleContext, eventTopics, this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(BundleContext bundleContext)
        throws Exception
    {
        if (eventHandlerRegistration != null)
        {
            eventHandlerRegistration.unregister();
            eventHandlerRegistration = null;
        }
    }
}
