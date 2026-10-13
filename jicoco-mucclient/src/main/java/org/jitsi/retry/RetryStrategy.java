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

import org.jitsi.utils.logging.*;

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
     * The runner of the current {@link #task}. Every call to
     * {@link #runRetryingTask(RetryTask)} creates a new runner, and a runner
     * which is no longer the current one does not schedule further attempts,
     * so only the current task keeps rescheduling itself. A superseded runner
     * may still be finishing an attempt which was already running.
     */
    private TaskRunner taskRunner;

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
        {
            throw new NullPointerException("executor");
        }

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

        if (task != null)
        {
            task.setCancelled(true);
        }
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
     * The given task replaces any task previously started with this method:
     * an attempt of the previous task which is still pending is cancelled, and
     * an attempt of the previous task which is currently running is not
     * interrupted, but will not schedule further attempts. At most one attempt
     * is pending at any time. With a multi-threaded executor, a superseded
     * attempt which is still running may overlap the new task's first attempt.
     *
     * @param task the retry task to be employed by this retry strategy instance
     */
    synchronized public void runRetryingTask(final RetryTask task)
    {
        if (task == null)
            throw new NullPointerException("task");

        if (future != null)
        {
            future.cancel(false);
        }

        this.task = task;
        this.taskRunner = new TaskRunner(task);
        this.future
            = executor.schedule(
                    taskRunner,
                    task.getInitialDelay(),
                    TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules a new retry attempt, unless the strategy has been cancelled or
     * <tt>runner</tt> has been superseded by a later call to
     * {@link #runRetryingTask(RetryTask)}.
     *
     * @param runner the runner which has just finished an attempt.
     */
    synchronized private void scheduleRetry(TaskRunner runner)
    {
        if (runner != taskRunner || runner.task.isCancelled())
            return;

        this.future
            =  executor.schedule(
                    runner,
                    runner.task.getRetryDelay(),
                    TimeUnit.MILLISECONDS);
    }

    /**
     * Some extra processing around running retry callable.
     */
    private class TaskRunner implements Runnable
    {
        /**
         * The task which this runner executes. It deliberately shadows
         * {@link RetryStrategy#task}: a superseded runner finishes the attempt
         * of its own task, not of the current one.
         */
        private final RetryTask task;

        TaskRunner(RetryTask task)
        {
            this.task = task;
        }

        @Override
        public void run()
        {
            try
            {
                if (task.getCallable().call())
                    scheduleRetry(this);
            }
            catch (Throwable t)
            {
                // Catch errors too: an error escaping from here would end the
                // retries without a trace, since nothing reads the future.
                logger.error(t, t);

                if (task.willRetryAfterException())
                    scheduleRetry(this);
            }
        }
    }
}
