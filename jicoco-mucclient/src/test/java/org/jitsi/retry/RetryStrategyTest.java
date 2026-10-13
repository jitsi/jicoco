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
package org.jitsi.retry;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jitsi.utils.concurrent.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class RetryStrategyTest
{
    @Test
    public void testRetryCount()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);

        long initialDelay = 150L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                    initialDelay, retryDelay, false, targetRetryCount);

        retryStrategy.runRetryingTask(retryTask);
        fakeExecutor.run();

        // Check if the task has not been executed before initial delay
        assertEquals(0, retryTask.counter);

        fakeExecutor.getClock().elapse(Duration.ofMillis(initialDelay + 10L));
        fakeExecutor.run();

        // Should be 1 after 1st pass
        assertEquals(1, retryTask.counter);

        // Now sleep two time retry delay
        fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay + 10L));
        fakeExecutor.run();
        fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay + 10L));
        fakeExecutor.run();
        assertEquals(3, retryTask.counter);

        // Sleep a bit more to check if it has stopped
        fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay + 10L));
        fakeExecutor.run();
        assertEquals(3, retryTask.counter);
    }

    @Test
    public void testRetryWithException()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);

        long initialDelay = 30L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                initialDelay, retryDelay, false, targetRetryCount);

        // Should throw an Exception on 2nd pass and stop
        retryTask.exceptionOnCount = 1;

        retryStrategy.runRetryingTask(retryTask);

        fakeExecutor.getClock().elapse(Duration.ofMillis(initialDelay + 10L));
        fakeExecutor.run();
        for (int i = 0; i < 3; i++)
        {
            fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay + 10L));
            fakeExecutor.run();
        }

        assertEquals(1, retryTask.counter);

        // Now modify strategy to not cancel on exception
        retryTask.reset();

        // Check if reset worked
        assertEquals(0, retryTask.counter);

        // Will fail at count = 1, but should continue
        retryTask.exceptionOnCount = 1;
        retryTask.setRetryAfterException(true);

        retryStrategy.runRetryingTask(retryTask);


        fakeExecutor.getClock().elapse(Duration.ofMillis(initialDelay + 10L));
        fakeExecutor.run();
        for (int i = 0; i < 4; i++)
        {
            fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay + 10L));
            fakeExecutor.run();
        }

        assertEquals(3, retryTask.counter);
    }

    @Test
    public void testCancel()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);

        long initialDelay = 30L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                    initialDelay, retryDelay, false, targetRetryCount);

        retryStrategy.runRetryingTask(retryTask);

        fakeExecutor.getClock().elapse(Duration.ofMillis(initialDelay + 10L));
        fakeExecutor.run();

        retryStrategy.cancel();

        assertEquals(1, retryTask.counter);

        fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay));
        fakeExecutor.run();
        fakeExecutor.getClock().elapse(Duration.ofMillis(retryDelay));
        fakeExecutor.run();

        assertEquals(1, retryTask.counter);
    }

    @Test
    public void testRunRetryingTaskReplacesPendingAttempt()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);

        long delay = 100L;
        TestCounterTask first = new TestCounterTask(delay, delay, false, Integer.MAX_VALUE);
        TestCounterTask second = new TestCounterTask(delay, delay, false, Integer.MAX_VALUE);

        retryStrategy.runRetryingTask(first);
        fakeExecutor.getClock().elapse(Duration.ofMillis(delay + 10L));
        fakeExecutor.run();
        assertEquals(1, first.counter);
        assertEquals(1, fakeExecutor.numPendingJobs());

        // Starting a new task drops the attempt still pending for the first one.
        retryStrategy.runRetryingTask(second);
        assertEquals(1, fakeExecutor.numPendingJobs());

        for (int i = 0; i < 5; i++)
        {
            // Advances the clock to the next live job and runs it.
            fakeExecutor.runOne();
            assertEquals(1, fakeExecutor.numPendingJobs());
        }

        assertEquals(1, first.counter);
        assertEquals(5, second.counter);
    }

    @Test
    public void testRunRetryingTaskFromRunningAttempt()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);

        long delay = 100L;
        AtomicInteger outerAttempts = new AtomicInteger();
        TestCounterTask inner = new TestCounterTask(0, delay, false, Integer.MAX_VALUE);
        // An attempt which starts a new task while it is running, and then asks to be retried itself.
        RetryTask outer = new SimpleRetryTask(0, delay, false, () ->
        {
            outerAttempts.incrementAndGet();
            retryStrategy.runRetryingTask(inner);
            return true;
        });

        retryStrategy.runRetryingTask(outer);
        fakeExecutor.run();

        assertEquals(1, outerAttempts.get());
        assertEquals(1, inner.counter);
        // Only the new task's chain is live.
        assertEquals(1, fakeExecutor.numPendingJobs());

        for (int i = 0; i < 10; i++)
        {
            fakeExecutor.getClock().elapse(Duration.ofMillis(delay));
            fakeExecutor.run();
            assertEquals(1, fakeExecutor.numPendingJobs());
        }

        assertEquals(1, outerAttempts.get());
        assertEquals(11, inner.counter);
    }

    @Test
    public void testDynamicRetryDelay()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);

        List<Instant> attempts = new ArrayList<>();
        RetryTask task = new SimpleRetryTask(0, 0, false, () ->
        {
            attempts.add(fakeExecutor.getClock().instant());
            return attempts.size() < 4;
        })
        {
            @Override
            public long getRetryDelay()
            {
                // 100, 200, 300
                return 100L * attempts.size();
            }
        };

        Instant start = fakeExecutor.getClock().instant();
        retryStrategy.runRetryingTask(task);
        fakeExecutor.runUntil(start.plusSeconds(10));

        assertEquals(4, attempts.size());
        assertEquals(Duration.ofMillis(100), Duration.between(attempts.get(0), attempts.get(1)));
        assertEquals(Duration.ofMillis(200), Duration.between(attempts.get(1), attempts.get(2)));
        assertEquals(Duration.ofMillis(300), Duration.between(attempts.get(2), attempts.get(3)));
    }

    @Test
    public void testCancelWhileAttemptRunning()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);
        AtomicInteger attempts = new AtomicInteger();

        // E.g. MucClient.stop() on another thread while an attempt to connect is in progress.
        retryStrategy.runRetryingTask(new SimpleRetryTask(0, 100, false, () ->
        {
            attempts.incrementAndGet();
            retryStrategy.cancel();
            return true;
        }));
        fakeExecutor.run();

        assertTrue(retryStrategy.isCancelled());
        assertEquals(0, fakeExecutor.numPendingJobs());
        fakeExecutor.runUntil(fakeExecutor.getClock().instant().plusSeconds(10));
        assertEquals(1, attempts.get());
    }

    @Test
    public void testCancelWhileSupersededAttemptRunning()
    {
        FakeScheduledExecutorService fakeExecutor = new FakeScheduledExecutorService();
        RetryStrategy retryStrategy = new RetryStrategy(fakeExecutor);
        AtomicInteger outerAttempts = new AtomicInteger();
        TestCounterTask inner = new TestCounterTask(100, 100, false, Integer.MAX_VALUE);

        retryStrategy.runRetryingTask(new SimpleRetryTask(0, 100, false, () ->
        {
            outerAttempts.incrementAndGet();
            retryStrategy.runRetryingTask(inner);
            retryStrategy.cancel();
            return true;
        }));
        fakeExecutor.run();

        assertEquals(0, fakeExecutor.numPendingJobs());
        fakeExecutor.runUntil(fakeExecutor.getClock().instant().plusSeconds(10));
        assertEquals(1, outerAttempts.get());
        assertEquals(0, inner.counter);
    }

    @Test
    public void testCancelBeforeStart()
    {
        RetryStrategy retryStrategy = new RetryStrategy(new FakeScheduledExecutorService());
        retryStrategy.cancel();
        assertFalse(retryStrategy.isCancelled());
    }

    private static class TestCounterTask
        extends RetryTask
    {
        int counter;

        int targetRetryCount;

        int exceptionOnCount = -1;

        public TestCounterTask(long               initialDelay,
                               long               retryDelay,
                               boolean            retryOnException,
                               int                targetRetryCount)
        {
            super(initialDelay, retryDelay, retryOnException);

            this.targetRetryCount = targetRetryCount;
        }

        public void reset()
        {
            counter = 0;
            exceptionOnCount = -1;
        }

        @Override
        public Callable<Boolean> getCallable()
        {
            return () ->
            {
                if (exceptionOnCount == counter)
                {
                    // Will not throw on next attempt
                    exceptionOnCount = -1;
                    // Throw error
                    throw new Exception("Simulated error in retry job");
                }

                // Retry as long as the counter stays below the target
                return ++counter < targetRetryCount;
            };
        }
    }
}
