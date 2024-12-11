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

/**
 * Simple implementation of {@link #getCallable()} which stores callable method
 * in the constructor.
 *
 * @author Pawel Domas
 */
public class SimpleRetryTask
    extends RetryTask
{
    /**
     * Retry job callable to be executed on each retry attempt.
     */
    protected Callable<Boolean> retryJob;

    /**
     * Initializes new instance of <tt>SimpleRetryTask</tt>.
     *
     * @param initialDelay how long we're going to wait before running task
     *                     callable for the first time(in ms).
     * @param retryDelay how often are we going to retry(in ms).
     * @param retryOnException should we continue retry after callable throws
     *                         unexpected <tt>Exception</tt>.
     * @param retryJob the callable job to be executed on retry.
     */
    public SimpleRetryTask(long               initialDelay,
                           long               retryDelay,
                           boolean            retryOnException,
                           Callable<Boolean>  retryJob)
    {
        super(initialDelay, retryDelay, retryOnException);

        this.retryJob = retryJob;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Callable<Boolean> getCallable()
    {
        return retryJob;
    }
}
