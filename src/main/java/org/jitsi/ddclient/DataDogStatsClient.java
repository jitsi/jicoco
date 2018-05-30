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
package org.jitsi.ddclient;

import com.timgroup.statsd.*;
import org.jitsi.util.*;

import java.util.*;

/**
 * Encapsulates a {@link NonBlockingStatsDClient} to be able to send
 * statistics to a DataDog server
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
     * The client to the datadog service
     */
    private NonBlockingStatsDClient innerClient;

    /**
     * The prefix which will always be placed before the aspect
     */
    private String prefix;

    /**
     * Create an instance of this {@link DataDogStatsClient}
     * by creating the {@link NonBlockingStatsDClient}
     *
     * @param prefix the prefix used in every call to datadog
     * @param domain the domain of the DataDog server
     * @param port the port the DataDog server is running on
     */
    public DataDogStatsClient(String prefix, String domain, int port)
    {
        this.innerClient = new NonBlockingStatsDClient(prefix, domain, port);
        this.prefix = prefix;

        if (logger.isInfoEnabled())
        {
            logger.info(String.format("registered datadog client with " +
                "prefix %s, domain %s and port %d", prefix, domain, port));
        }
    }

    /**
     * Increment a given statistic (aspect) which is being kept track off
     *
     * @param aspect the aspect to increment
     * @param tags optional tags to send along with increasing this counter
     */
    public void incrementCounter(String aspect, String... tags)
    {
        innerClient.incrementCounter(aspect, tags);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Incrementing statsd counter " +
                    "(prefix: %s, aspect: %s, tags: %s)",
                prefix, aspect, Arrays.toString(tags)));
        }
    }

    /**
     * Stop the client, disabling further calls
     */
    public void stop()
    {
        this.innerClient.stop();
    }
}
