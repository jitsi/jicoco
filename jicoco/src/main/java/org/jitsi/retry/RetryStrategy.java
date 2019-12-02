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

import org.jitsi.osgi.*;
import org.jitsi.utils.logging.*;
import org.osgi.framework.*;

import java.util.concurrent.*;

/**
 * A retry strategy for doing some job. It allows to specify initial delay
 * before the task is started as well as retry delay. It will be executed as
 * long as the task <code>Callable<Boolean></code> returns <tt>true</tt>. It is
 * also possible to configure whether the retries should be continued after
 * unexpected exception or not.
 * If we decide to not continue retries from outside the task
 * {@link RetryStrategy#cancel()} method will prevent from scheduling future
 * retries(but it will not interrupt currently executing one). Check with
 * {@link RetryStrategy#isCancelled()} to stop the operation in progress.
 *
 * See "RetryStrategyTest" for usage samples.
 *
 * @author Pawel Domas
 */
public class RetryStrategy
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(RetryStrategy.class);

    /**
     * Scheduled executor service used to schedule retry task.
     */
    private final ScheduledExecutorService executor;

    /**
     * <tt>RetryTask</tt> instance which describes the retry task and provides
     * things like retry interval and callable method to be executed.
     */
    private RetryTask task;

    /**
     * Future instance used to eventually cancel the retry task.
     */
    private ScheduledFuture<?> future;

    /**
     * Inner class implementing <tt>Runnable</tt> that does additional
     * processing around Callable retry job.
     */
    private final TaskRunner taskRunner = new TaskRunner();


    /**
     * Creates new <tt>RetryStrategy</tt> instance that will use
     * <tt>ScheduledExecutorService</tt> with pool size of 1 thread to schedule
     * retry attempts.
     */
    public RetryStrategy()
    {
        this(Executors.newScheduledThreadPool(1));
    }

    /**
     * Creates new <tt>RetryStrategy</tt> instance that will use
     * <tt>ScheduledExecutorService</tt> obtained from given OSGi
     * <tt>osgiContext</tt>.
     *
     * @param osgiContext OSGi context that should contain registered
     *                    <tt>ScheduledExecutorService</tt> service instance.
     *
     * @throws IllegalArgumentException if given <tt>osgiContext</tt> does not
     *         have any <tt>ScheduledExecutorService</tt> service instances
     *         registered.
     */
    public RetryStrategy(BundleContext osgiContext)
        throws IllegalArgumentException
    {
        this.executor
            = ServiceUtils2.getService(
                    osgiContext,
                    ScheduledExecutorService.class);

        if (executor == null)
            throw new IllegalArgumentException(
                "Failed to obtain ScheduledExecutorService" +
                    " from given OSGi context");
    }

    /**
     * Creates new instance of <tt>RetryStrategy</tt> that will use given
     * <tt>ScheduledExecutorService</tt> to schedule retry attempts.
     *
     * @param retryExecutor <tt>ScheduledExecutorService</tt> that will be used
     *                      for scheduling retry attempts.
     *
     * @throws NullPointerException if given <tt>retryExecutor</tt> is
     *         <tt>null</tt>
     */
    public RetryStrategy(ScheduledExecutorService retryExecutor)
    {
        if (retryExecutor == null)
            throw new NullPointerException("executor");

        this.executor = retryExecutor;
    }

    /**
     * Cancels any future retry attempts. Currently running tasks are not
     * interrupted.
     */
    synchronized public void cancel()
    {
        if (future != null)
        {
            future.cancel(false);
            future = null;
        }

        task.setCancelled(true);
    }

    /**
     * Returns <tt>true</tt> if this retry strategy has been cancelled or
     * <tt>false</tt> otherwise.
     */
    synchronized public boolean isCancelled()
    {
        return task != null && task.isCancelled();
    }

    /**
     * Start given <tt>RetryTask</tt> that will be executed for the first time
     * after {@link RetryTask#getInitialDelay()}. After first execution next
     * retry attempts will be rescheduled as long as it's callable method
     * returns <tt>true</tt> or until ({@link #cancel()} is called.
     *
     * @param task the retry task to be employed by this retry strategy instance
     */
    synchronized public void runRetryingTask(final RetryTask task)
    {
        if (task == null)
            throw new NullPointerException("task");

        this.task = task;
        this.future
            = executor.schedule(
                    taskRunner,
                    task.getInitialDelay(),
                    TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules new retry attempt if we d
     */
    synchronized private void scheduleRetry()
    {
        if (task == null || task.isCancelled())
            return;

        this.future
            =  executor.schedule(
                    taskRunner,
                    task.getRetryDelay(),
                    TimeUnit.MILLISECONDS);
    }

    /**
     * Some extra processing around running retry callable.
     */
    class TaskRunner implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                if (task.getCallable().call())
                    scheduleRetry();
            }
            catch (Exception e)
            {
                logger.error(e, e);

                if (task.willRetryAfterException())
                    scheduleRetry();
            }
        }
    }
}
