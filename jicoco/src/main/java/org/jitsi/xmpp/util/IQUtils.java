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
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.*;
import org.jxmpp.jid.impl.*;
import org.xmlpull.v1.*;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Packet;

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
     * The <tt>XmlPullParserFactory</tt> instance which is to create
     * <tt>XmlPullParser</tt> instances for the purposes of
     * {@link #convert(org.xmpp.packet.IQ)}. Introduced as a shared instance in
     * order to avoid unnecessary allocations.
     */
    private static XmlPullParserFactory xmlPullParserFactory;

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
     * Converts a specific <tt>org.xmpp.packet.IQ</tt> instance into a new
     * <tt>org.jivesoftware.smack.packet.IQ</tt> instance which represents the
     * same stanza.
     *
     * @param iq the <tt>org.xmpp.packet.IQ</tt> instance to convert to a new
     * <tt>org.jivesoftware.smack.packet.IQ</tt> instance
     * @return a new <tt>org.jivesoftware.smack.packet.IQ</tt> instance which
     * represents the same stanza as the specified <tt>iq</tt>
     * @throws Exception if anything goes wrong during the conversion
     */
    // It is safe to cast as ProviderManager verifies classes
    // the warning is about cast 'providerOrClass' var to '? extends IQ'
    @SuppressWarnings("unchecked")
    public static org.jivesoftware.smack.packet.IQ convert(
            org.xmpp.packet.IQ iq)
        throws Exception
    {
        Element element = iq.getChildElement();
        IQProvider<org.jivesoftware.smack.packet.IQ> iqProvider = null;

        if (element != null)
        {
            iqProvider
                = ProviderManager.getIQProvider(
                        element.getName(),
                        element.getNamespaceURI());
        }

        IQ.Type type = iq.getType();
        org.jivesoftware.smack.packet.IQ smackIQ = null;
        org.jivesoftware.smack.packet.XMPPError.Builder smackError = null;

        if (iqProvider != null || iq.getError() != null)
        {
            XmlPullParserFactory xmlPullParserFactory;

            synchronized (IQUtils.class)
            {
                if (IQUtils.xmlPullParserFactory == null)
                {
                    IQUtils.xmlPullParserFactory
                        = XmlPullParserFactory.newInstance();
                    IQUtils.xmlPullParserFactory.setNamespaceAware(true);
                }
                xmlPullParserFactory = IQUtils.xmlPullParserFactory;
            }

            XmlPullParser parser = xmlPullParserFactory.newPullParser();

            parser.setInput(new StringReader(iq.toXML()));

            int eventType = parser.next();
            // Abort processing if we're not at the beginning of <iq> element
            if (XmlPullParser.START_TAG != eventType)
            {
                throw new IllegalStateException(
                    Integer.toString(eventType)
                        + " != XmlPullParser.START_TAG");
            }

            String name = parser.getName();
            // Stop processing if the element is not <iq>
            if (!"iq".equals(name))
            {
                throw new IllegalStateException(name + " != iq");
            }

            do
            {
                eventType = parser.next();
                name = parser.getName();
                if (XmlPullParser.START_TAG == eventType)
                {
                    // 7. An IQ stanza of type "error" MAY include the
                    // child element contained in the associated "get"
                    // or "set" and MUST include an <error/> child.
                    if (IQ.Type.error.equals(type) && "error".equals(name))
                    {
                        smackError = PacketParserUtils.parseError(parser);
                    }
                    else if (smackIQ == null && iqProvider != null)
                    {
                        smackIQ = iqProvider.parse(parser);
                    }
                }
                else if ((XmlPullParser.END_TAG == eventType
                            && "iq".equals(name))
                        || (smackIQ != null && smackError != null)
                        || XmlPullParser.END_DOCUMENT == eventType)
                {
                    break;
                }
            }
            while (true);

            // Throw an exception if we have not consumed the whole <iq> element
            eventType = parser.getEventType();
            if (XmlPullParser.END_TAG != eventType)
            {
                throw new IllegalStateException(
                        Integer.toString(eventType)
                            + " != XmlPullParser.END_TAG");
            }
        }

        // 6. An IQ stanza of type "result" MUST include zero or one child
        // elements.
        // 7. An IQ stanza of type "error" MAY include the child element
        // contained in the associated "get" or "set" and MUST include an
        // <error/> child.
        if (smackIQ == null
                && (IQ.Type.error.equals(type) || IQ.Type.result.equals(type)))
        {
            smackIQ = new org.jivesoftware.smack.packet.IQ((String)null)
            {
                @Override
                protected IQChildElementXmlStringBuilder
                    getIQChildElementBuilder(IQChildElementXmlStringBuilder xml)
                {
                    return xml;
                }
            };
        }

        if (smackIQ != null)
        {
            // from
            org.xmpp.packet.JID fromJID = iq.getFrom();

            if (fromJID != null)
                smackIQ.setFrom(JidCreate.from(fromJID.toString()));
            // id
            smackIQ.setStanzaId(iq.getID());

            // to
            org.xmpp.packet.JID toJID = iq.getTo();

            if (toJID != null)
                smackIQ.setTo(JidCreate.from(toJID.toString()));
            // type
            smackIQ.setType(convert(type));

            if (smackError != null)
                smackIQ.setError(smackError);
        }

        return smackIQ;
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
     * Converts an <tt>org.xmpp.packet.IQ.Type</tt> value into an
     * <tt>org.jivesoftware.smack.packet.IQ.Type</tt> value which represents the
     * same IQ type.
     *
     * @param type the <tt>org.xmpp.packet.IQ.Type</tt> value to convert into an
     * <tt>org.jivesoftware.smack.packet.IQ.Type</tt> value
     * @return an <tt>org.jivesoftware.smack.packet.IQ.Type</tt> value which
     * represents the same IQ type as the specified <tt>type</tt>
     */
    public static org.jivesoftware.smack.packet.IQ.Type convert(
            org.xmpp.packet.IQ.Type type)
    {
        return org.jivesoftware.smack.packet.IQ.Type.fromString(type.name());
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

    /**
     * Methods used for IQProvider testing. Parses given XML string with given
     * <tt>IQProvider</tt>.
     * @param iqStr XML string to be parsed
     * @param iqProvider the IQProvider which will be used to parse the IQ.
     * @return the IQ parsed from given <tt>iqStr</tt> if parsable by given
     *         <tt>iqProvider</tt>.
     * @throws Exception if anything goes wrong
     */
    public static <T extends org.jivesoftware.smack.packet.IQ> T parse(
        String iqStr,
        IQProvider<T> iqProvider)
        throws Exception
    {
        T smackIQ = null;

        if (iqProvider != null)
        {
            XmlPullParserFactory xmlPullParserFactory;

            synchronized (IQUtils.class)
            {
                if (IQUtils.xmlPullParserFactory == null)
                {
                    IQUtils.xmlPullParserFactory
                        = XmlPullParserFactory.newInstance();
                    IQUtils.xmlPullParserFactory.setNamespaceAware(true);
                }
                xmlPullParserFactory = IQUtils.xmlPullParserFactory;
            }

            XmlPullParser parser = xmlPullParserFactory.newPullParser();

            parser.setInput(new StringReader(iqStr));

            int eventType = parser.next();

            if (XmlPullParser.START_TAG == eventType)
            {
                String name = parser.getName();

                if ("iq".equals(name))
                {
                    String packetId = parser.getAttributeValue("", "id");
                    String from = parser.getAttributeValue("", "from");
                    String to = parser.getAttributeValue("", "to");
                    String type = parser.getAttributeValue("", "type");

                    eventType = parser.next();
                    if (XmlPullParser.START_TAG == eventType)
                    {
                        smackIQ = iqProvider.parse(parser);

                        if (smackIQ != null)
                        {
                            eventType = parser.getEventType();
                            if (XmlPullParser.END_TAG != eventType)
                            {
                                throw new IllegalStateException(
                                    Integer.toString(eventType)
                                        + " != XmlPullParser.END_TAG");
                            }

                            smackIQ.setType(
                                org.jivesoftware.smack.packet.IQ.Type
                                    .fromString(type));
                            smackIQ.setStanzaId(packetId);
                            smackIQ.setFrom(JidCreate.from(from));
                            smackIQ.setTo(JidCreate.from(to));
                        }
                    }
                    else
                    {
                        throw new IllegalStateException(
                            Integer.toString(eventType)
                                + " != XmlPullParser.START_TAG");
                    }
                }
                else
                    throw new IllegalStateException(name + " != iq");
            }
            else
            {
                throw new IllegalStateException(
                    Integer.toString(eventType)
                        + " != XmlPullParser.START_TAG");
            }
        }

        return smackIQ;
    }

    /**
     * Utility method which can be used for logging XML of response packets.
     * It deals with the timeout case where the response is <tt>null</tt>.
     *
     * @param response a <tt>Packet</tt> instance returned by XMPP connection as
     *        a response to some request.
     *
     * @return {@link Packet#toXML()} or "(timeout)" <tt>String</tt> if given
     *         <tt>response</tt> is <tt>null</tt>.
     */
    public static String responseToXML(
            org.jivesoftware.smack.packet.Stanza response)
    {
        return response != null ? response.toXML().toString() : "(timeout)";
    }

    /** Prevents the initialization of new <tt>IQUtils</tt> instances. */
    private IQUtils()
    {
    }
}
