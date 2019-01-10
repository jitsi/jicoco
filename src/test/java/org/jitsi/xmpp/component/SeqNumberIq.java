/*
 * Copyright @ 2016 Atlassian Pty Ltd
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
package org.jitsi.xmpp.component;

import org.dom4j.*;
import org.xmpp.packet.*;

/**
 * A test IQ which contains 'send-seq-num' and 'recv-seq-num' attributes in
 * the child element for testing packet processing order.
 *
 * @author Pawel Domas
 */
public class SeqNumberIq
    extends IQ
{
    public static final String NAMESPACE = "jitsi:iq:test";

    public static final String ELEMENT_NAME = "testiq";

    public static final String RECV_SEQ_NUM = "recv-seq-num";

    public static final String SEND_SEQ_NUM = "send-seq-num";

    private final Element childElement;

    public SeqNumberIq() {
        super();

        this.childElement = element.addElement(ELEMENT_NAME, NAMESPACE);
    }

    public String getRecvSeqNum()
    {
        return childElement.attributeValue(RECV_SEQ_NUM);
    }

    public void setRecvSeqNum(String seqNum)
    {
        childElement.addAttribute(RECV_SEQ_NUM, seqNum);
    }

    public String getSendSeqNum()
    {
        return childElement.attributeValue(SEND_SEQ_NUM);
    }

    public void setSendSeqNum(String seqNum)
    {
        childElement.addAttribute(SEND_SEQ_NUM, seqNum);
    }
}
