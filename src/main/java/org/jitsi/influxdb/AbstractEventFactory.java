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
package org.jitsi.influxdb;

import java.util.*;

/**
 * Class gathers stuff common for event factories.
 */
public class AbstractEventFactory
{
    /**
     * The name of the topic of a "endpoint display name changed" event.
     */
    public static final String ENDPOINT_DISPLAY_NAME_CHANGED_TOPIC
        = "org/jitsi/videobridge/Endpoint/NAME_CHANGED";

    public static final String EVENT_SOURCE = "event.source";

    /**
     * Creates <tt>Dictionary<String, Object></tt> that contains {@link #EVENT_SOURCE} object.
     * @param source the {@link #EVENT_SOURCE} object to be included in the
     *               dictionary.
     */
    protected static Dictionary<String, Object> makeProperties(Object source)
    {
        Dictionary<String, Object> properties = new Hashtable<String, Object>(1);

        properties.put(EVENT_SOURCE, source);

        return properties;
    }
}
