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
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.DoubleSupplier;
import org.jitsi.retry.*;
import org.jitsi.utils.concurrent.*;
import org.jivesoftware.smack.sasl.*;
import org.jivesoftware.smack.sasl.packet.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class AuthFailureBackoffTest
{
    /**
     * Always produces the midpoint of [0, 1), i.e. delays without jitter.
     */
    private static final DoubleSupplier NO_JITTER = () -> 0.5;

    private static final long[] EXPECTED_DELAYS = { 1000, 2000, 4000, 8000, 16000, 32000, 60000, 60000, 60000 };

    @Test
    public void testGrowthAndCap()
    {
        MucClient.AuthFailureBackoff backoff = new MucClient.AuthFailureBackoff(1000, 60_000, NO_JITTER);
        assertEquals(0, backoff.getDelayMs());
        assertEquals(0, backoff.getFailures());

        for (int i = 0; i < EXPECTED_DELAYS.length; i++)
        {
            assertEquals(EXPECTED_DELAYS[i], backoff.recordFailure());
            assertEquals(EXPECTED_DELAYS[i], backoff.getDelayMs());
            assertEquals(i + 1, backoff.getFailures());
        }

        // A client which keeps failing for a long time stays at the cap.
        for (int i = 0; i < 1000; i++)
        {
            backoff.recordFailure();
        }
        assertEquals(60_000, backoff.getDelayMs());
    }

    @Test
    public void testJitter()
    {
        assertEquals(900, new MucClient.AuthFailureBackoff(1000, 60_000, () -> 0.0).recordFailure());
        assertEquals(1100, new MucClient.AuthFailureBackoff(1000, 60_000, () -> 0.999_999).recordFailure());

        MucClient.AuthFailureBackoff backoff = new MucClient.AuthFailureBackoff(1000, 60_000);
        Set<Long> delays = new HashSet<>();
        for (int i = 0; i < 100; i++)
        {
            backoff.reset();
            long delay = backoff.recordFailure();
            assertTrue(delay >= 900 && delay <= 1100, "delay out of range: " + delay);
            delays.add(delay);
        }
        assertTrue(delays.size() > 1, "delays are not randomized");
    }

    @Test
    public void testReset()
    {
        MucClient.AuthFailureBackoff backoff = new MucClient.AuthFailureBackoff(1000, 60_000, NO_JITTER);
        assertEquals(0, backoff.reset());

        backoff.recordFailure();
        backoff.recordFailure();
        backoff.recordFailure();
        assertEquals(4000, backoff.getDelayMs());

        assertEquals(3, backoff.reset());
        assertEquals(0, backoff.getDelayMs());
        assertEquals(0, backoff.getFailures());
        assertEquals(0, backoff.reset());

        // After a successful login, the next rejection starts from the initial delay again.
        assertEquals(1000, backoff.recordFailure());
    }

    @Test
    public void testInvalidLimits()
    {
        // The maximum is never below the initial delay.
        MucClient.AuthFailureBackoff backoff = new MucClient.AuthFailureBackoff(5000, 1000, NO_JITTER);
        assertEquals(5000, backoff.recordFailure());
        assertEquals(5000, backoff.recordFailure());

        // Delays are always positive.
        assertEquals(1, new MucClient.AuthFailureBackoff(0, -1, NO_JITTER).recordFailure());
    }

    @Test
    public void testFindAuthFailure()
    {
        for (SASLError error : new SASLError[] {
            SASLError.not_authorized, SASLError.account_disabled, SASLError.credentials_expired })
        {
            SASLErrorException e = saslError(error);
            assertSame(e, MucClient.findAuthFailure(e), error.toString());
            assertSame(e, MucClient.findAuthFailure(new RuntimeException("wrapped", e)), error.toString());
        }

        for (SASLError error : new SASLError[] {
            SASLError.temporary_auth_failure, SASLError.invalid_mechanism, SASLError.aborted })
        {
            assertNull(MucClient.findAuthFailure(saslError(error)), error.toString());
        }

        assertNull(MucClient.findAuthFailure(new IOException("Connection reset")));
        assertNull(MucClient.findAuthFailure(null));
    }

    /**
     * Drives {@link MucClient.ConnectAndLoginTask} with a {@link RetryStrategy} on a fake executor, with a login
     * which the server always rejects, and checks that attempts back off instead of running in a tight loop.
     */
    @Test
    public void testRejectedLoginsBackOff()
    {
        FakeScheduledExecutorService executor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(executor);
        MucClient.AuthFailureBackoff backoff = new MucClient.AuthFailureBackoff(1000, 60_000, NO_JITTER);

        List<Instant> attempts = new ArrayList<>();
        AtomicBoolean serverAcceptsLogin = new AtomicBoolean(false);
        Callable<Boolean> connectAndLogin = () ->
        {
            attempts.add(executor.getClock().instant());
            if (serverAcceptsLogin.get())
            {
                backoff.reset();
                return false;
            }
            // What MucClient's connect-and-login callable does when the server rejects the login.
            backoff.recordFailure();
            return true;
        };

        Instant start = executor.getClock().instant();
        retryStrategy.runRetryingTask(new MucClient.ConnectAndLoginTask(0, 5000, backoff, connectAndLogin));
        executor.runUntil(start.plus(Duration.ofMinutes(10)));

        // 1+2+4+8+16+32 seconds, then once a minute: 15 attempts in 10 minutes, where the old loop made thousands.
        assertEquals(15, attempts.size());
        assertEquals(start, attempts.get(0));
        for (int i = 1; i < attempts.size(); i++)
        {
            long expected = EXPECTED_DELAYS[Math.min(i - 1, EXPECTED_DELAYS.length - 1)];
            assertEquals(expected, Duration.between(attempts.get(i - 1), attempts.get(i)).toMillis(), "attempt " + i);
        }
        assertEquals(1, executor.numPendingJobs());

        // A connection close which re-arms the retry with no initial delay (as connectionClosed() does) replaces the
        // pending attempt, and still waits for the backoff delay.
        Instant rearmed = executor.getClock().instant();
        int attemptsBefore = attempts.size();
        retryStrategy.runRetryingTask(new MucClient.ConnectAndLoginTask(0, 1000, backoff, connectAndLogin));
        assertEquals(1, executor.numPendingJobs());
        executor.runOne();
        assertEquals(attemptsBefore + 1, attempts.size());
        assertEquals(60_000, Duration.between(rearmed, attempts.get(attempts.size() - 1)).toMillis());

        // Once the login succeeds the retry stops, and the backoff is back to the fast path.
        serverAcceptsLogin.set(true);
        executor.runOne();
        assertEquals(0, executor.numPendingJobs());
        assertEquals(0, backoff.getDelayMs());

        // A later re-arm (e.g. after a network drop) starts immediately and uses the normal retry delay.
        serverAcceptsLogin.set(false);
        Instant restarted = executor.getClock().instant();
        attemptsBefore = attempts.size();
        retryStrategy.runRetryingTask(new MucClient.ConnectAndLoginTask(0, 1000, backoff, connectAndLogin));
        executor.run();
        assertEquals(attemptsBefore + 1, attempts.size());
        assertEquals(restarted, attempts.get(attempts.size() - 1));
        assertEquals(1000, backoff.getDelayMs());
    }

    @Test
    public void testConfiguration()
    {
        MucClientConfiguration config = new MucClientConfiguration("id");
        assertEquals(MucClientConfiguration.DEFAULT_AUTH_FAILURE_INITIAL_BACKOFF_MS,
            config.getAuthFailureInitialBackoffMs());
        assertEquals(MucClientConfiguration.DEFAULT_AUTH_FAILURE_MAX_BACKOFF_MS, config.getAuthFailureMaxBackoffMs());

        // Property names are case-insensitive, as for the other properties.
        config.setProperty("auth_failure_initial_backoff_ms", "500");
        config.setProperty(MucClientConfiguration.AUTH_FAILURE_MAX_BACKOFF_MS, " 30000 ");
        assertEquals(500, config.getAuthFailureInitialBackoffMs());
        assertEquals(30_000, config.getAuthFailureMaxBackoffMs());

        config.setAuthFailureInitialBackoffMs(2000);
        assertEquals(2000, config.getAuthFailureInitialBackoffMs());

        config.setProperty(MucClientConfiguration.AUTH_FAILURE_INITIAL_BACKOFF_MS, "soon");
        config.setProperty(MucClientConfiguration.AUTH_FAILURE_MAX_BACKOFF_MS, "0");
        assertEquals(MucClientConfiguration.DEFAULT_AUTH_FAILURE_INITIAL_BACKOFF_MS,
            config.getAuthFailureInitialBackoffMs());
        assertEquals(MucClientConfiguration.DEFAULT_AUTH_FAILURE_MAX_BACKOFF_MS, config.getAuthFailureMaxBackoffMs());
    }

    private static SASLErrorException saslError(SASLError error)
    {
        return new SASLErrorException("PLAIN", new SaslNonza.SASLFailure(error.toString()));
    }
}
