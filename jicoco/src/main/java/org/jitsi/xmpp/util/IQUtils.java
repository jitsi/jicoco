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

import org.jivesoftware.smack.packet.*;

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
     * org.jivesoftware.smack.packet.IQ, XMPPError.Condition, String)} with
     * no error message text.
     *
     * @see #createError(org.jivesoftware.smack.packet.IQ, XMPPError.Condition,
     * String)
     */
    public static IQ createError(IQ request, XMPPError.Condition errorCondition)
    {
        return createError(request, errorCondition, null);
    }

    /**
     * A shortcut for <tt>new XMPPError(request,
     * new XMPPError(errorCondition, errorMessage));</tt>. Create error response
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
    public static IQ createError(IQ request, XMPPError.Condition errorCondition, String errorMessage)
    {
        XMPPError.Builder error = XMPPError.getBuilder(errorCondition);
        if (errorMessage != null)
        {
            error.setDescriptiveEnText(errorMessage);
        }

        return IQ.createErrorResponse(request, error);
    }

    /** Prevents the initialization of new <tt>IQUtils</tt> instances. */
    private IQUtils()
    {
    }
}
