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

import org.dom4j.*;
import org.dom4j.Element;
import org.dom4j.io.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.*;

import java.io.*;

/**
 * Provides functionality which aids the manipulation of
 * <tt>org.jivesoftware.smack.packet.IQ</tt> and <tt>org.xmpp.packet.IQ</tt>
 * instances.
 *
 * @Deprecated
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 * @author Pawel Domas
 */
public final class IQUtils
{
    /**
     * Converts a specific <tt>org.jivesoftware.smack.packet.IQ</tt> instance
     * into a new <tt>org.xmpp.packet.iQ</tt> instance which represents the same
     * stanza.
     *
     * @param smackIQ the <tt>org.jivesoftware.smack.packet.IQ</tt> instance to
     * convert to a new <tt>org.xmpp.packet.IQ</tt> instance
     * @return a new <tt>org.xmpp.packet.IQ</tt> instance which represents the
     * same stanza as the specified <tt>smackIQ</tt>
     * @throws Exception if anything goes wrong during the conversion
     */
    public static org.xmpp.packet.IQ convert(
            org.jivesoftware.smack.packet.IQ smackIQ)
        throws Exception
    {
        XmlStringBuilder xml = smackIQ.toXML();

        SAXReader saxReader = new SAXReader();
        Document document = saxReader.read(new StringReader(xml.toString()));
        Element element = document.getRootElement();

        org.xmpp.packet.IQ iq = new org.xmpp.packet.IQ(element);

        iq.setID(smackIQ.getStanzaId());
        iq.setType(convert(smackIQ.getType()));

        return iq;
    }

    /**
     * Converts an <tt>org.jivesoftware.smack.packet.IQ.Type</tt> value into an
     * <tt>org.xmpp.packet.IQ.Type</tt> value which represents the same IQ type.
     *
     * @param smackType the <tt>org.jivesoftware.smack.packet.IQ.Type</tt> value
     * to convert into an <tt>org.xmpp.packet.IQ.Type</tt> value
     * @return an <tt>org.xmpp.packet.IQ.Type</tt> value which represents the
     * same IQ type as the specified <tt>smackType</tt>
     */
    public static org.xmpp.packet.IQ.Type convert(
            org.jivesoftware.smack.packet.IQ.Type smackType)
    {
        return org.xmpp.packet.IQ.Type.valueOf(smackType.toString());
    }

    /**
     * Method overload for {@link #createError(
     * org.jivesoftware.smack.packet.IQ, XMPPError.Condition, String)} with
     * no error message text.
     *
     * @see #createError(org.jivesoftware.smack.packet.IQ, XMPPError.Condition,
     * String)
     */
    public static org.jivesoftware.smack.packet.IQ createError(
            org.jivesoftware.smack.packet.IQ    request,
            XMPPError.Condition                 errorCondition)
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
    public static org.jivesoftware.smack.packet.IQ createError(
            org.jivesoftware.smack.packet.IQ    request,
            XMPPError.Condition                 errorCondition,
            String                              errorMessage)
    {
        XMPPError.Builder error = XMPPError.getBuilder(errorCondition);
        if (errorMessage != null)
        {
            error.setDescriptiveEnText(errorMessage);
        }

        return org.jivesoftware.smack.packet.IQ.createErrorResponse(
                request, error);
    }

    /** Prevents the initialization of new <tt>IQUtils</tt> instances. */
    private IQUtils()
    {
    }
}
