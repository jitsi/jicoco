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

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.jitsi.retry.RetryStrategy.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetryStrategyTest
{
    @Test
    public void testRetryCount()
    {
        AtomicInteger scheduleCalls = new AtomicInteger();
        AtomicReference<TaskRunner> runner = new AtomicReference<>();
        ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);
        when(mockedExecutor.schedule(any(TaskRunner.class), anyLong(), eq(TimeUnit.MILLISECONDS))).then(a -> {
            scheduleCalls.getAndIncrement();
            runner.set((TaskRunner) a.getArguments()[0]);
            return null;
        });
        RetryStrategy retryStrategy = new RetryStrategy(mockedExecutor);

        long initialDelay = 150L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                    initialDelay, retryDelay, false, targetRetryCount);

        // must not schedule on construction
        assertEquals(0, scheduleCalls.get());

        // do schedule now
        retryStrategy.runRetryingTask(retryTask);
        assertEquals(1, scheduleCalls.get());

        // Check that is scheduled, not immediately executed on the same thread
        assertEquals(0, retryTask.counter);

        // simulate execution
        runner.get().run();

        // Should be 1 after 1st pass
        assertEquals(1, retryTask.counter);

        // Now schedule and run two more times
        assertEquals(2, scheduleCalls.get());
        runner.get().run();
        assertEquals(3, scheduleCalls.get());
        runner.get().run();
        assertEquals(3, retryTask.counter);

        // check if it has stopped
        assertEquals(3, scheduleCalls.get());
        assertEquals(3, retryTask.counter);
    }

    @Test
    public void testRetryWithException()
    {
        AtomicInteger scheduleCalls = new AtomicInteger();
        AtomicReference<TaskRunner> runner = new AtomicReference<>();
        ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);
        when(mockedExecutor.schedule(any(TaskRunner.class), anyLong(), eq(TimeUnit.MILLISECONDS))).then(a -> {
            scheduleCalls.getAndIncrement();
            runner.set((TaskRunner) a.getArguments()[0]);
            return null;
        });
        RetryStrategy retryStrategy = new RetryStrategy(mockedExecutor);

        long initialDelay = 30L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                initialDelay, retryDelay, false, targetRetryCount);

        // Should throw an Exception on 2nd pass and stop
        retryTask.exceptionOnCount = 1;

        retryStrategy.runRetryingTask(retryTask);
        assertEquals(1, scheduleCalls.get());
        runner.get().run();
        assertEquals(2, scheduleCalls.get());
        runner.get().run();

        assertEquals(1, retryTask.counter);

        // Now modify strategy to not cancel on exception
        retryTask.reset();

        // Check if reset worked
        assertEquals(0, retryTask.counter);

        // Will fail at count = 1, but should continue
        retryTask.exceptionOnCount = 1;
        retryTask.setRetryAfterException(true);

        retryStrategy.runRetryingTask(retryTask);
        assertEquals(3, scheduleCalls.get());
        runner.get().run();

        assertEquals(4, scheduleCalls.get());
        runner.get().run();
        assertEquals(5, scheduleCalls.get());
        runner.get().run();
        assertEquals(6, scheduleCalls.get());
        runner.get().run();

        assertEquals(3, retryTask.counter);
    }

    @Test
    public void testCancel()
    {
        AtomicInteger scheduleCalls = new AtomicInteger();
        AtomicReference<TaskRunner> runner = new AtomicReference<>();
        ScheduledExecutorService mockedExecutor = mock(ScheduledExecutorService.class);
        when(mockedExecutor.schedule(any(TaskRunner.class), anyLong(), eq(TimeUnit.MILLISECONDS))).then(a -> {
            scheduleCalls.getAndIncrement();
            runner.set((TaskRunner) a.getArguments()[0]);
            return null;
        });
        RetryStrategy retryStrategy = new RetryStrategy(mockedExecutor);

        long initialDelay = 30L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                    initialDelay, retryDelay, false, targetRetryCount);

        retryStrategy.runRetryingTask(retryTask);
        assertEquals(1, scheduleCalls.get());
        runner.get().run();
        assertEquals(1, retryTask.counter);
        assertEquals(2, scheduleCalls.get());

        retryStrategy.cancel();
        assertEquals(2, scheduleCalls.get());
        assertTrue(retryTask.isCancelled());
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
            return new Callable<Boolean>()
            {
                @Override
                public Boolean call()
                    throws Exception
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
                }
            };
        }
    }

}
