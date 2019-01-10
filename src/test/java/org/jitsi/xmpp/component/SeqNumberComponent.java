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

import java.util.*;
import java.util.concurrent.*;

/**
 * A component used in the {@link ComponentBaseTest#testStripeExecutor()}.
 *
 * Emulates packets processing by blocking the processing thread. The
 * processing time range is adjusted with {@link #minProcessingTime} and
 * {@link #maxProcessingTime}.
 *
 * Every packet source (based on the IQ's 'from' field) gets assigned a
 * separate {@link SeqNumberHandler} which keeps track of the packets
 * processing sequence. It produces {@link SeqNumberIq} responses with the
 * {@link SeqNumberIq#RECV_SEQ_NUM} attribute set to the number which reflects
 * the order in which the packet was processed by the corresponding handler.
 */
public class SeqNumberComponent
    extends ComponentBase
{
    /**
     * The response packets produced by this component end up on this queue
     * which reflects the order in which the packets have been sent.
     */
    private final BlockingQueue<Packet> queue = new LinkedBlockingQueue<>();

    private long minProcessingTime;

    private long maxProcessingTime;

    /**
     * A separate {@link SeqNumberHandler} is created for every packet source.
     * A packet's source is identified by the 'from' field value of the IQ.
     */
    private final Map<String, SeqNumberHandler> handlers = new Hashtable<>();

    /**
     * Flag used to disable Stripe generation in order to fail the test.
     */
    private boolean useStripes = true;

    public SeqNumberComponent(
        String host, int port, String domain, String subDomain,
        String secret)
    {
        super(host, port, domain, subDomain, secret);
    }

    @Override
    public String getDescription()
    {
        return "A test component implementation";
    }

    @Override
    public String getName()
    {
        return "debug";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.xmpp.component.AbstractComponent#send(org.xmpp.packet.Packet)
     */
    @Override
    protected void send(Packet packet) {
        queue.add(packet);
    }

    private SeqNumberHandler getHandler(String from)
    {
        synchronized (handlers)
        {
            SeqNumberHandler handler = handlers.get(from);
            if (handler == null)
            {
                handler = new SeqNumberHandler();
                handlers.put(from, handler);
            }
            return handler;
        }
    }

    @Override
    protected IQ handleIQGetImpl(IQ iq)
        throws Exception
    {
        final Element childElement = iq.getChildElement();
        String namespace = null;

        if (childElement != null)
        {
            namespace = childElement.getNamespaceURI();
        }

        if (SeqNumberIq.NAMESPACE.equals(namespace))
        {
            SeqNumberHandler handler = getHandler(iq.getFrom().toString());

            return handler.handleIq(iq);
        }
        else
        {
            return super.handleIQGetImpl(iq);
        }
    }

    /**
     * Returns the first packet that's sent using the {@link #send(Packet)}
     * method and that has not been returned by earlier calls to this method.
     * This method will block for up to two seconds if no packets have been
     * sent yet.
     *
     * @return A sent packet.
     * @throws InterruptedException
     */
    public Packet getSentPacket() throws InterruptedException {
        return queue.poll(2, TimeUnit.SECONDS);
    }

    public void setMinProcessingTime(long minProcessingTime)
    {
        this.minProcessingTime = minProcessingTime;
    }

    public void setMaxProcessingTime(long maxProcessingTime)
    {
        this.maxProcessingTime = maxProcessingTime;

        this.processingTimeLimit = 2 * maxProcessingTime;
    }

    public void resetHandlers()
    {
        handlers.clear();
    }


    private final Map<String, Object> map = new WeakHashMap<>();

    @Override
    protected Object getStripeForPacket(Packet p)
    {
        if (!useStripes)
        {
            return null;
        }

        Object stripe;
        String from = p.getFrom().toString();

        synchronized (map)
        {
            stripe = map.computeIfAbsent(from, f -> "Stripe for: " + f);
        }

        return stripe;
    }

    public void disableStripes()
    {
        this.useStripes = false;
    }

    class SeqNumberHandler
    {
        /**
         * The lack of synchronization on the counter is intentional. The
         * component which uses
         * {@link eu.javaspecialists.tjsn.concurrency.stripedexecutor.StripedExecutorService}
         * guarantees that packets which come from the single source (the
         * 'from' field) are processed in order and only one at a time.
         */
        private int counter = 0;

        SeqNumberIq handleIq(IQ iq)
            throws InterruptedException
        {
            SeqNumberIq response = new SeqNumberIq();
            response.setType(IQ.Type.result);
            response.setID(iq.getID());
            response.setTo(iq.getFrom());
            response.setFrom(iq.getTo());

            final Element childElement = iq.getChildElement();
            String senderSequence
                = childElement.attribute(SeqNumberIq.SEND_SEQ_NUM).getValue();

            response.setSendSeqNum(senderSequence);
            response.setRecvSeqNum(String.valueOf(++counter));

            log.debug(
                    String.format("Executing %s[R: %s, S:%s] by %s",
                                  response.getTo(),
                                  response.getRecvSeqNum(),
                                  response.getSendSeqNum(),
                                  Thread.currentThread()));

            Thread.sleep(
                    ThreadLocalRandom.current().nextLong(
                            minProcessingTime, maxProcessingTime));

            return response;
        }
    }
}
