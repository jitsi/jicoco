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
 * Class describes a retry task executed by {@link RetryStrategy}.
 * It has the following properties:
 * <li>{@link #initialDelay} - specifies the time before this task is launched
 * for the first time</li>
 * <li>{@link #retryDelay} - tells how much time we wait before next retry
 * attempt. Subclass can override {@link #getRetryDelay()} in order to provide
 * dynamic value which can be different for each retry</li>
 * <li>{@link #getCallable()}</li> - a <code>Callable<Boolean></code> which is
 * the job to be executed by retry strategy. The task will be retried as long as
 * it returns <tt>true</tt> or until the job is cancelled.
 * <li>{@link #retryAfterException}</li> - indicates if retries should be
 * continued after uncaught exception is thrown by retry callable task
 * <li>{@link #cancelled}</li> - indicates if {@link RetryStrategy} and this
 * task has been cancelled using {@link RetryStrategy#cancel()}. This does not
 * interrupt currently executing task.
 *
 * @author Pawel Domas
 */
public abstract class RetryTask
{
    /**
     * Value in ms. Specifies the time before this task is launched for
     * the first time.
     */
    private final long initialDelay;

    /**
     * Value in ms. Tells how much time we wait before next retry attempt.
     */
    private final long retryDelay;

    /**
     * Indicates if retries should be continued after uncaught exception is
     * thrown by retry callable task.
     */
    private boolean retryAfterException;

    /**
     * Indicates if {@link RetryStrategy} and this task has been cancelled using
     * {@link RetryStrategy#cancel()}. This does not interrupt currently
     * executing task.
     */
    private boolean cancelled;

    /**
     * Initializes new instance of <tt>RetryTask</tt>.
     * @param initialDelay how long we're going to wait before running task
     *                     callable for the first time(in ms).
     * @param retryDelay how often are we going to retry(in ms).
     * @param retryOnException should we continue retry after callable throws
     *                         unexpected <tt>Exception</tt>.
     */
    public RetryTask(long       initialDelay,
                     long       retryDelay,
                     boolean    retryOnException)
    {
        this.initialDelay = initialDelay;
        this.retryDelay = retryDelay;
        this.retryAfterException = retryOnException;
    }

    /**
     * Returns the time in ms before this task is launched for the first time.
     */
    public long getInitialDelay()
    {
        return initialDelay;
    }

    /**
     * Returns the delay in ms that we wait before next retry attempt.
     */
    public long getRetryDelay()
    {
        return retryDelay;
    }

    /**
     * Returns a <code>Callable<Boolean></code> which is the job to be executed
     * by retry strategy. The task will be retried as long as it returns
     * <tt>true</tt> or until the job is cancelled.
     */
    abstract public Callable<Boolean> getCallable();

    /**
     * Indicates if we're going to continue retry task scheduling after the
     * callable throws unexpected exception.
     */
    public boolean willRetryAfterException()
    {
        return retryAfterException;
    }

    /**
     * Should we continue retries after the callable throws unexpected exception
     * ?
     * @param retryAfterException <tt>true</tt> to continue retries even though
     *        unexpected exception is thrown by the callable, otherwise retry
     *        strategy will be cancelled when that happens.
     */
    public void setRetryAfterException(boolean retryAfterException)
    {
        this.retryAfterException = retryAfterException;
    }

    /**
     * Returns <tt>true</tt> if this task has been cancelled.
     */
    public boolean isCancelled()
    {
        return cancelled;
    }

    /**
     * Method is called by <tt>RetryStrategy</tt> when it gets cancelled.
     * @param cancelled <tt>true</tt> when this task is being cancelled.
     */
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }
}
