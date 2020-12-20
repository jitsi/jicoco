/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.health

import org.jitsi.utils.concurrent.PeriodicRunnable
import org.jitsi.utils.concurrent.RecurringRunnableExecutor
import org.jitsi.utils.logging2.Logger
import org.jitsi.utils.logging2.LoggerImpl
import org.jitsi.utils.secs
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.properties.Delegates

/**
 * A [HealthCheckService] implementation which checks health via the provided
 * [healthCheckFunc] function.
 */
class HealthChecker(
    /**
     * The interval at which health checks will be performed.
     */
    interval: Duration = 10.secs,
    /**
     * If no health checks have been performed in the last {@code timeout}
     * period, the service is considered unhealthy.
     */
    var timeout: Duration = 30.secs,
    /**
     * The maximum duration that a call to {@link #performCheck()} is allowed
     * to take. If a call takes longer, the service is considered unhealthy.
     * <p>
     * Note that if a check never completes, we rely on {@link #timeout} instead.
     */
    var maxCheckDuration: Duration = 3.secs,
    /**
     * If set, a single health check failure after the initial
     * {@link #STICKY_FAILURES_GRACE_PERIOD} will be result in the service
     * being permanently unhealthy.
     */
    var stickyFailures: Boolean = false,
    /**
     * Failures in this period (since the start of the service) are not sticky.
     */
    var stickyFailuresGracePeriod: Duration = stickyFailuresGracePeriodDefault,
    private val healthCheckFunc: () -> Unit,
    private val clock: Clock = Clock.systemUTC()
) : HealthCheckService, PeriodicRunnable(interval.toMillis()) {
    private val logger: Logger = LoggerImpl(javaClass.name)

    /**
     * The executor which runs {@link #performCheck()} periodically.
     */
    private var executor: RecurringRunnableExecutor? = null

    /**
     * The exception resulting from the last health check. When the health
     * check is successful, this is {@code null}.
     */
    private var lastResult: Exception? = null

    /**
     * The time the last health check finished being performed.
     */
    private var lastResultTime = Instant.MIN

    /**
     * The time when this service was started.
     */
    private var serviceStartTime = Instant.MAX

    /**
     * Whether we've seen a health check failure (after the grace period).
     */
    private var hasFailed = false

    /**
     * The interval at which health checks will be performed.
     */
    var interval: Duration by Delegates.observable(interval) {
        _, _, newValue ->
        period = newValue.toMillis()
    }

    /**
     * Returns the result of the last performed health check, or a new exception
     * if no health check has been perform recently.
     * @return
     */
    override fun getResult(): Exception? {
        val timeSinceLastResult: Duration = Duration.between(lastResultTime, clock.instant())
        if (timeSinceLastResult > timeout) {
            return Exception("No health checks performed recently, the last result was $timeSinceLastResult ago.")
        }
        return lastResult
    }

    fun start() {
        if (executor == null) {
            executor = RecurringRunnableExecutor(javaClass.name)
        }
        executor!!.registerRecurringRunnable(this)

        logger.info(
            "Started with interval=$period, timeout=$timeout, " +
                "maxDuration=$maxCheckDuration, stickyFailures=$stickyFailures."
        )
    }

    @Throws(Exception::class)
    fun stop() {
        executor?.apply {
            deRegisterRecurringRunnable(this@HealthChecker)
            close()
        }
        executor = null
        logger.info("Stopped")
    }

    /**
     * Performs a health check and updates this instance's state. Runs
     * periodically in {@link #executor}.
     */
    override fun run() {
        super.run()

        val checkStart = clock.instant()
        var exception: Exception? = null

        try {
            healthCheckFunc()
        } catch (e: Exception) {
            exception = e

            val now = clock.instant()
            val timeSinceStart = Duration.between(serviceStartTime, now)
            if (timeSinceStart > stickyFailuresGracePeriod) {
                hasFailed = true
            }
        }

        lastResultTime = clock.instant()
        val checkDuration = Duration.between(checkStart, lastResultTime)
        if (checkDuration > maxCheckDuration) {
            exception = Exception("Performing a health check took too long: $checkDuration")
        }

        lastResult = if (stickyFailures && hasFailed && exception == null) {
            // We didn't fail this last test, but we've failed before and
            // sticky failures are enabled.
            Exception("Sticky failure.")
        } else {
            exception
        }

        if (exception == null) {
            logger.info(
                "Performed a successful health check in $checkDuration. Sticky failure: ${stickyFailures && hasFailed}"
            )
        } else {
            logger.error("Health check failed in $checkDuration:", exception)
        }
    }

    companion object {
        val stickyFailuresGracePeriodDefault = Duration.ofMinutes(5)
    }
}
