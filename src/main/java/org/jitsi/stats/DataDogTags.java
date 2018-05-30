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
 * This class saves tags which specify for which service an aspect
 * is incremented when it uses
 * {@link DataDogStatsClient#incrementCounter(String, String...)}
 *
 * @author Nik Vaessen
 */
public class DataDogTags
{
    /**
     * Tag for the recording service
     */
    public final static String TAG_SERVICE_RECORDING = "recording";

    /**
     * Tag for the livestreaming service
     */
    public final static String TAG_SERVICE_LIVE_STREAM = "live_stream";

    /**
     * Tag for the sip gataway service
     */
    public final static String TAG_SERVICE_SIP_GATEWAY = "sip_gateway";

    /**
     * Tag for the transcription service
     */
    public final static String TAG_SERVICE_TRANSCRIBING = "transcribing";
}