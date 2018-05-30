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
 */
package org.jitsi.stats;

import com.timgroup.statsd.*;
import org.jitsi.util.*;

/**
 * Encapsulates a {@link NonBlockingStatsDClient} to be able to send
 * statistics to local data-hog server
 *
 * @author Nik Vaessen
 */
public class DataDogStatsClient
{
    /**
     * The logger of this class
     */
    private static final Logger logger
        = Logger.getLogger(DataDogStatsClient.class);

    /**
     * Single instance of this {@link DataDogStatsClient}
     */
    private static DataDogStatsClient singleton = new DataDogStatsClient();

    /**
     * The client to the data-hog service
     */
    private NonBlockingStatsDClient innerClient;

    /**
     * Create the single instance of this {@link DataDogStatsClient}
     * by creating the {@link NonBlockingStatsDClient}
     */
    private DataDogStatsClient()
    {
        this.innerClient = new NonBlockingStatsDClient(
            "jicofo",
            "localhost",
            8125
        );
    }

    /**
     * Get the single instance of this {@link DataDogStatsClient}
     *
     * @return the DataDogStatsClient
     */
    public static DataDogStatsClient getClient()
    {
        return singleton;
    }

    /**
     * Increment a given statistic (aspect) which is being kept track off
     *
     * @param aspect the aspect to increment
     * @param tags optional tags to send along with increasing this counter
     */
    public final void incrementCounter(String aspect, String... tags)
    {
        innerClient.incrementCounter(aspect, tags);
    }
}
