/*
 * Copyright @ 2018 - present, 8x8 Inc
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
 *
 */
package org.jitsi.xmpp.mucclient;

import org.jivesoftware.smack.packet.*;

/**
 * An interface for handling IQs coming from a specific {@link MucClient}.
 *
 * @author Boris Grozev
 */
public interface IQListener
{
    /**
     * Handles an IQ. Default implementation which ignores the {@link MucClient}
     * which from which the IQ comes.
     *
     * @param iq the IQ to be handled.
     * @return the IQ to be sent as a response or {@code null}.
     */
    default IQ handleIq(IQ iq)
    {
        return null;
    }

    /**
     * Handles an IQ. Default implementation which ignores the {@link MucClient}
     * which from which the IQ comes.
     *
     * @param iq the IQ to be handled.
     * @param mucClient the {@link MucClient} from which the IQ comes.
     * @return the IQ to be sent as a response or {@code null}.
     */
    default IQ handleIq(IQ iq, MucClient mucClient)
    {
        return handleIq(iq);
    }
}
