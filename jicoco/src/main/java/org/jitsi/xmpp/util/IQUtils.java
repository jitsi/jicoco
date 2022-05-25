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
package org.jitsi.xmpp.util;

import org.jetbrains.annotations.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.xml.*;
import org.jxmpp.jid.impl.*;

import java.io.*;

/**
 * Provides functionality which aids the manipulation of
 * <tt>org.jivesoftware.smack.packet.IQ</tt> and <tt>org.xmpp.packet.IQ</tt>
 * instances.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 * @author Pawel Domas
 */
public final class IQUtils
{
    /**
     * Method overload for {@link #createError(
     * org.jivesoftware.smack.packet.IQ, StanzaError.Condition, String)} with
     * no error message text.
     *
     * @see #createError(org.jivesoftware.smack.packet.IQ, StanzaError.Condition,
     * String)
     */
    public static IQ createError(IQ request, StanzaError.Condition errorCondition)
    {
        return createError(request, errorCondition, null);
    }

    /**
     * A shortcut for <tt>new StanzaError(request,
     * new StanzaError(errorCondition, errorMessage));</tt>. Create error response
     * to given <tt>request</tt> IQ.
     *
     * @param request the request IQ for which the error response will be
     *                created.
     * @param errorCondition the XMPP error condition for the error response.
     * @param errorMessage the error text message to be included in the error
     *                     response.
     *
     * @return an IQ which is an XMPP error response to given <tt>request</tt>.
     */
    public static IQ createError(IQ request, StanzaError.Condition errorCondition, String errorMessage)
    {
        StanzaError.Builder error = StanzaError.getBuilder(errorCondition);
        if (errorMessage != null)
        {
            error.setDescriptiveEnText(errorMessage);
        }

        return org.jivesoftware.smack.packet.IQ.createErrorResponse(
                request, error.build());
    }

    /** Prevents the initialization of new <tt>IQUtils</tt> instances. */
    private IQUtils()
    {
    }
}
