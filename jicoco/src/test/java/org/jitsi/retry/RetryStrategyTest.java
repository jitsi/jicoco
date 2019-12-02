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

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;

/**
 *
 */
@RunWith(JUnit4.class)
public class RetryStrategyTest
{
    @Test
    public void testRetryCount()
        throws InterruptedException
    {
        RetryStrategy retryStrategy = new RetryStrategy();

        long initialDelay = 150L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                    initialDelay, retryDelay, false, targetRetryCount);

        retryStrategy.runRetryingTask(retryTask);

        // Check if the task has not been executed before initial delay
        assertEquals(0, retryTask.counter);

        Thread.sleep(initialDelay + 10L);

        // Should be 1 after 1st pass
        assertEquals(1, retryTask.counter);

        // Now sleep two time retry delay
        Thread.sleep(retryDelay * 2L + 10L);
        assertEquals(3, retryTask.counter);

        // Sleep a bit more to check if it has stopped
        Thread.sleep(retryDelay + 10L);
        assertEquals(3, retryTask.counter);

    }

    @Test
    public void testRetryWithException()
        throws InterruptedException
    {
        RetryStrategy retryStrategy = new RetryStrategy();

        long initialDelay = 30L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                initialDelay, retryDelay, false, targetRetryCount);

        // Should throw an Exception on 2nd pass and stop
        retryTask.exceptionOnCount = 1;

        retryStrategy.runRetryingTask(retryTask);

        Thread.sleep(initialDelay + 3L * retryDelay + 20L);

        assertEquals(1, retryTask.counter);

        // Now modify strategy to not cancel on exception
        retryTask.reset();

        // Check if reset worked
        assertEquals(0, retryTask.counter);

        // Will fail at count = 1, but should continue
        retryTask.exceptionOnCount = 1;
        retryTask.setRetryAfterException(true);

        retryStrategy.runRetryingTask(retryTask);

        Thread.sleep(initialDelay + 4L * retryDelay);

        assertEquals(3 , retryTask.counter);
    }

    @Test
    public void testCancel()
        throws InterruptedException
    {
        RetryStrategy retryStrategy = new RetryStrategy();

        long initialDelay = 30L;
        long retryDelay = 50L;
        int targetRetryCount = 3;

        TestCounterTask retryTask
            = new TestCounterTask(
                    initialDelay, retryDelay, false, targetRetryCount);

        retryStrategy.runRetryingTask(retryTask);

        Thread.sleep(initialDelay + 10L);

        retryStrategy.cancel();

        assertEquals(1, retryTask.counter);

        Thread.sleep(2L * retryDelay);

        assertEquals(1, retryTask.counter);
    }

    private class TestCounterTask
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
                        throw new Exception("Unexpected error in retry job");
                    }

                    // Retry as long as the counter stays below the target
                    return ++counter < targetRetryCount;
                }
            };
        }
    }

}
