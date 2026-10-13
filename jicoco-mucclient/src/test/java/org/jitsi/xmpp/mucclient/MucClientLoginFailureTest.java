/*
 * Copyright @ 2026 - present, 8x8 Inc
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
package org.jitsi.xmpp.mucclient;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.regex.*;
import javax.security.auth.callback.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.sasl.*;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Drives a real {@link MucClient} against a fake XMPP server on the loopback interface, which answers SASL auth
 * requests with a failure. This covers what {@link AuthFailureBackoffTest} can't: the connect-and-login callable's
 * catch block, and the {@code connectionClosed()} listener which used to turn every failed login into a tight loop.
 */
public class MucClientLoginFailureTest
{
    private static final Pattern BIND_REQUEST = Pattern.compile("<iq[^>]*id=['\"]([^'\"]+)['\"][^>]*>\\s*<bind");

    private static int originalReplyTimeout;

    @BeforeAll
    public static void setUpSmack()
    {
        // smack-java8, which would provide a Base64 encoder, is not on the test classpath.
        Base64.setEncoder(new Base64.Encoder()
        {
            @Override
            public byte[] decode(String string)
            {
                return java.util.Base64.getDecoder().decode(string);
            }

            @Override
            public String encodeToString(byte[] input)
            {
                return java.util.Base64.getEncoder().encodeToString(input);
            }

            @Override
            public String encodeToStringWithoutPadding(byte[] input)
            {
                return java.util.Base64.getEncoder().withoutPadding().encodeToString(input);
            }

            @Override
            public byte[] encode(byte[] input)
            {
                return java.util.Base64.getEncoder().encode(input);
            }
        });
        SASLAuthentication.registerSASLMechanism(new TestMechanism());

        // Don't wait 5s for the MUC join, which the fake server never answers, after a successful login.
        originalReplyTimeout = SmackConfiguration.getDefaultReplyTimeout();
        SmackConfiguration.setDefaultReplyTimeout(500);
    }

    @AfterAll
    public static void tearDownSmack()
    {
        SASLAuthentication.unregisterSASLMechanism(TestMechanism.class.getName());
        SmackConfiguration.setDefaultReplyTimeout(originalReplyTimeout);
    }

    @Test
    public void testRejectedLoginsBackOff()
        throws Exception
    {
        try (FakeServer server = new FakeServer("not-authorized"))
        {
            // Attempts at about 0, 200, 600, 1400 and 2200 ms.
            runClient(server, 200, 800, 2500);
            int attempts = server.authRequests.get();
            assertTrue(attempts >= 3 && attempts <= 6, "attempts: " + attempts);
        }
    }

    @Test
    public void testOtherLoginFailuresDoNotLoop()
        throws Exception
    {
        try (FakeServer server = new FakeServer("temporary-auth-failure"))
        {
            // Not an auth failure, so no backoff, but the disconnect after the failed login must not start an
            // immediate retry: the initial task's 5s retry delay applies. The old code made thousands of attempts.
            runClient(server, 200, 800, 2500);
            int attempts = server.authRequests.get();
            assertTrue(attempts >= 1 && attempts <= 2, "attempts: " + attempts);
        }
    }

    @Test
    public void testSuccessfulLoginResetsBackoff()
        throws Exception
    {
        try (FakeServer server = new FakeServer("not-authorized"))
        {
            server.acceptAfter = 3;
            MucClientManager manager = new MucClientManager();
            manager.addMucClient(config(server.port(), 100, 400));
            try
            {
                MucClient.AuthFailureBackoff backoff = manager.getMucClient("test").getAuthFailureBackoff();
                // The server counts the bind when it replies, the client resets the backoff once it has processed
                // the reply.
                waitFor(() -> server.binds.get() >= 1 && backoff.getFailures() == 0, 5000);

                assertEquals(1, server.binds.get());
                assertEquals(4, server.authRequests.get());
                assertEquals(0, backoff.getFailures());
                assertEquals(0, backoff.getDelayMs());
            }
            finally
            {
                manager.stop();
            }
        }
    }

    @Test
    public void testServerCloseAfterRecoveredLoginReconnects()
        throws Exception
    {
        try (FakeServer server = new FakeServer("not-authorized"))
        {
            server.acceptAfter = 1;
            server.closeAfterFirstBind = true;
            MucClientManager manager = new MucClientManager();
            manager.addMucClient(config(server.port(), 100, 400));
            try
            {
                // One rejected login, one successful login, and when the server then closes the stream, the
                // connectionClosed() workaround still reconnects and logs in again.
                waitFor(() -> server.binds.get() >= 2, 8000);
                assertEquals(2, server.binds.get());
            }
            finally
            {
                manager.stop();
            }
        }
    }

    private static void runClient(FakeServer server, long initialBackoffMs, long maxBackoffMs, long durationMs)
        throws Exception
    {
        MucClientManager manager = new MucClientManager();
        manager.addMucClient(config(server.port(), initialBackoffMs, maxBackoffMs));
        try
        {
            Thread.sleep(durationMs);
        }
        finally
        {
            manager.stop();
        }
    }

    private static void waitFor(Callable<Boolean> condition, long timeoutMs)
        throws Exception
    {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.call() && System.currentTimeMillis() < deadline)
        {
            Thread.sleep(20);
        }
    }

    private static MucClientConfiguration config(int port, long initialBackoffMs, long maxBackoffMs)
    {
        MucClientConfiguration config = new MucClientConfiguration("test");
        config.setHostname("127.0.0.1");
        config.setPort(Integer.toString(port));
        config.setDomain("example.com");
        config.setUsername("user");
        config.setPassword("pass");
        config.setMucJids(Collections.singletonList("room@conference.example.com"));
        config.setMucNickname("nick");
        config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
        config.setAuthFailureInitialBackoffMs(initialBackoffMs);
        config.setAuthFailureMaxBackoffMs(maxBackoffMs);
        return config;
    }

    /**
     * A trivial SASL mechanism, which the fake server offers, so that no real mechanism has to succeed.
     */
    private static class TestMechanism
        extends SASLMechanism
    {
        @Override
        protected void authenticateInternal(CallbackHandler callbackHandler)
        {
        }

        @Override
        protected byte[] getAuthenticationText()
        {
            return new byte[] { 'x' };
        }

        @Override
        public String getName()
        {
            return "X-TEST";
        }

        @Override
        public int getPriority()
        {
            return 1;
        }

        @Override
        protected void checkIfSuccessfulOrThrow()
        {
        }

        @Override
        protected SASLMechanism newInstance()
        {
            return new TestMechanism();
        }

        @Override
        public boolean authzidSupported()
        {
            return false;
        }
    }

    /**
     * Just enough of an XMPP server: it offers {@code X-TEST}, answers auth requests with a SASL failure (or with
     * success after {@link #acceptAfter} failures), and answers resource binding.
     */
    private static class FakeServer
        implements Closeable
    {
        private final ServerSocket serverSocket;

        private final String failureCondition;

        private final ExecutorService pool = Executors.newCachedThreadPool();

        final AtomicInteger authRequests = new AtomicInteger();

        final AtomicInteger binds = new AtomicInteger();

        volatile int acceptAfter = Integer.MAX_VALUE;

        volatile boolean closeAfterFirstBind = false;

        FakeServer(String failureCondition)
            throws IOException
        {
            this.failureCondition = failureCondition;
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            pool.execute(this::acceptConnections);
        }

        int port()
        {
            return serverSocket.getLocalPort();
        }

        private void acceptConnections()
        {
            while (!serverSocket.isClosed())
            {
                try
                {
                    Socket socket = serverSocket.accept();
                    pool.execute(() -> handle(socket));
                }
                catch (IOException e)
                {
                    return;
                }
            }
        }

        private void handle(Socket socket)
        {
            try (Socket s = socket)
            {
                InputStream in = s.getInputStream();
                OutputStream out = s.getOutputStream();
                StringBuilder buf = new StringBuilder();

                readStreamHeader(in, buf);
                write(out, streamHeader("s1")
                    + "<stream:features><mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>"
                    + "<mechanism>X-TEST</mechanism></mechanisms></stream:features>");

                byte[] bytes = new byte[4096];
                int n;
                while ((n = in.read(bytes)) > 0)
                {
                    buf.append(new String(bytes, 0, n, StandardCharsets.UTF_8));

                    int auth = buf.indexOf("<auth");
                    if (auth >= 0)
                    {
                        buf.delete(0, auth + "<auth".length());
                        if (authRequests.incrementAndGet() > acceptAfter)
                        {
                            write(out, "<success xmlns='urn:ietf:params:xml:ns:xmpp-sasl'/>");
                            buf.setLength(0);
                            readStreamHeader(in, buf);
                            buf.setLength(0);
                            write(out, streamHeader("s2")
                                + "<stream:features><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'/></stream:features>");
                        }
                        else
                        {
                            write(out, "<failure xmlns='urn:ietf:params:xml:ns:xmpp-sasl'><" + failureCondition
                                + "/></failure>");
                        }
                    }

                    Matcher bind = BIND_REQUEST.matcher(buf);
                    if (bind.find())
                    {
                        String id = bind.group(1);
                        buf.delete(0, bind.end());
                        write(out, "<iq type='result' id='" + id + "'><bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>"
                            + "<jid>user@example.com/r</jid></bind></iq>");
                        if (binds.incrementAndGet() == 1 && closeAfterFirstBind)
                        {
                            Thread.sleep(300);
                            // A server-initiated stream close, as in the FIXME in MucClient's connectionClosed().
                            write(out, "</stream:stream>");
                        }
                    }

                    if (buf.indexOf("</stream:stream>") >= 0)
                    {
                        write(out, "</stream:stream>");
                        return;
                    }
                }
            }
            catch (IOException | InterruptedException ignored)
            {
                // The client went away, or the test is over.
            }
        }

        private static void readStreamHeader(InputStream in, StringBuilder buf)
            throws IOException
        {
            byte[] bytes = new byte[4096];
            while (buf.indexOf("<stream:stream") < 0 || buf.indexOf(">", buf.indexOf("<stream:stream")) < 0)
            {
                int n = in.read(bytes);
                if (n <= 0)
                {
                    throw new EOFException();
                }
                buf.append(new String(bytes, 0, n, StandardCharsets.UTF_8));
            }
        }

        private static String streamHeader(String id)
        {
            return "<?xml version='1.0'?><stream:stream xmlns='jabber:client' "
                + "xmlns:stream='http://etherx.jabber.org/streams' id='" + id + "' from='example.com' version='1.0'>";
        }

        private static void write(OutputStream out, String xml)
            throws IOException
        {
            out.write(xml.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }

        @Override
        public void close()
            throws IOException
        {
            serverSocket.close();
            pool.shutdownNow();
        }
    }
}
