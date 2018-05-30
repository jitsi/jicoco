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

/**
 * This class saves aspects which are being kept track of by the datadog client
 *
 * @author Nik Vaessen
 */
public class DataDogAspects
{
    /**
     * Aspect when something is started
     */
    private final static String ASPECT_START = "start";

    /**
     * Aspect when something is stopped
     */
    private final static String ASPECT_STOP = "stop";

    /**
     * Aspect when something is busy
     */
    private final static String ASPECT = "busy";

    /**
     * Aspect when something gave an error
     */
    private final static String ASPECT_ERROR = "error";
}
