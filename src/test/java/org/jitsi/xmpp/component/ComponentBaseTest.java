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

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import org.xmpp.packet.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for the {@link ComponentBase} class.
 *
 * @author Pawel Domas
 */
@RunWith(JUnit4.class)
public class ComponentBaseTest
{
    /**
     * At least how long the component will be processing each packet.
     */
    final static long MIN_PROCESSING_TIME = 5;

    /**
     * The packet processing time limit.
     */
    final static long MAX_PROCESSING_TIME = 20;

    /**
     * How many senders will be used in a test.
     */
    final static int SENDERS_COUNT = 3;

    /**
     * How many packets each sender will send in a burst.
     */
    final static int BURST_COUNT = 10;

    /**
     * How many burst will be tested.
     */
    final static int BURST_LENGTH = 10;

    /**
     * Tests the packet processing order of the XMPP component backend
     * implementation. The test consists of few bursts of packets being
     * injected into the component for processing. Each packet contains "senders
     * sequence number" which corresponds to the exact order of the packets sent
     * by each sender. Then the component has separate packet handler maintained
     * for each sender which tracks the "receivers sequence number" and puts
     * it into the response packets. The implementation of
     * {@link SeqNumberComponent#getStripeForPacket(Packet)} enforces
     * serial processing for each source, but allows packets from multiple
     * sources to be processed in parallel. At the end of the test,
     * the response packets are checked if senders sequence number is equal
     * to receivers sequence number which means that the packets have been
     * process in the order they were received.
     */
    @Test
    public void testStripeExecutor()
        throws InterruptedException
    {
        //JID componentJid = new JID("test-component.test-xmpp-domain.com");
        SeqNumberComponent component
            = new SeqNumberComponent(
                    "test.server.net", 1234,
                    "test-xmpp-domain.com",
                    "test-component", "secret");

        // Start the component
        component.start();

        component.setMinProcessingTime(MIN_PROCESSING_TIME);
        component.setMaxProcessingTime(MAX_PROCESSING_TIME);

        // Check if the packets are processed in order
        doProcessingOrderTest(component , false /* expect test failure */);


        // Enable parallel processing without the policy
        component.disableStripes();
        // The test should fail
        doProcessingOrderTest(component , true /* expect test failure */);
    }

    private void doProcessingOrderTest(SeqNumberComponent component,
                                       boolean expectFailure)
        throws InterruptedException
    {
        // Dispose of old packet processor handlers
        component.resetHandlers();

        // Initialize the senders
        TestSender[] senders = new TestSender[SENDERS_COUNT];
        for (int i = 0; i < SENDERS_COUNT; i++)
        {
            senders[i] = new TestSender(i);
        }

        // Generate test packet bursts
        int burstCount = BURST_COUNT;
        int burstLength = BURST_LENGTH;
        for (int burstNum = 0; burstNum < burstCount; burstNum++)
        {
            for (TestSender sender : senders)
            {
                for (int packetIdx = 0; packetIdx < burstLength; packetIdx++)
                {
                    component.processPacket(
                        sender.getNextTestIQ(component.getJID()));
                }
            }
            // Wait some time to avoid overfilling the thread executor queue
            final int burstSize = burstLength * senders.length;
            Thread.sleep(burstSize * MIN_PROCESSING_TIME / 2);
        }

        // Verify the processing sequence
        final int totalPacketCount = burstCount * burstLength * senders.length;
        for (int i = 0; i < totalPacketCount; i++)
        {
            Packet response = component.getSentPacket();

            assertTrue(response instanceof SeqNumberIq);

            SeqNumberIq testResponse = (SeqNumberIq) response;

            // Sanity check if the component's handlers have been cleared,
            // before next test
            int rcvSeqNumber = Integer.parseInt(testResponse.getRecvSeqNum());
            assertTrue(
                "Unexpected rcv sequence number: " + rcvSeqNumber,
                 rcvSeqNumber <= (totalPacketCount / SENDERS_COUNT));

            if (!expectFailure)
            {
                assertEquals(
                    testResponse.getRecvSeqNum(), testResponse.getSendSeqNum());
            }
            else if (!testResponse.getRecvSeqNum().equals(
                            testResponse.getSendSeqNum()))
            {
                // Found failing element - finish the test
                return;
            }
        }
        if (expectFailure)
        {
            fail("The test shouldn't succeed");
        }
    }

    static private JID createJidForNumber(int number)
    {
        return new JID(String.format("jid%d@server.com", number));
    }

    class TestSender
    {
        private int counter = 0;

        private final JID jid;

        public TestSender(int number)
        {
            this.jid = createJidForNumber(number);
        }

        private SeqNumberIq getNextTestIQ(JID to)
        {
            SeqNumberIq seqNumberIq = new SeqNumberIq();
            seqNumberIq.setTo(to);
            seqNumberIq.setFrom(this.jid);
            seqNumberIq.setType(IQ.Type.get);

            seqNumberIq.setSendSeqNum(String.valueOf(++counter));

            return seqNumberIq;
        }
    }
}
