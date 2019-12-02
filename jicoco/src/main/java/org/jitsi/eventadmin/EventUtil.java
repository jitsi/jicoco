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
package org.jitsi.eventadmin;

import org.osgi.framework.*;

import java.util.*;

/**
 * Utility methods for "eventadmin" package.
 *
 * @author Pawel Domas
 */
public class EventUtil
{
    /**
     * Registers <tt>EventHandler</tt> service instance that will be handling
     * given event topics.
     * @param ctx OSGi bundle context instance on which the event handler
     *            service will be registered.
     * @param topics an array of event topics that will be handled by given
     *               <tt>EventHandler</tt> service instance.
     * @param handler the instance of <tt>EventHandler</tt> service which will
     *                be registered on given OSGi context.
     * @return <tt>ServiceRegistration</tt> instance.
     */
    static public ServiceRegistration<EventHandler> registerEventHandler(
        BundleContext ctx, String[] topics, EventHandler handler)
    {
        Dictionary<String, Object> props = new Hashtable<String, Object>();

        props.put(EventConstants.EVENT_TOPIC, topics);

        return ctx.registerService(
            EventHandler.class,
            handler,
            props);
    }
}
