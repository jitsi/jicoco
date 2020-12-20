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

import org.jitsi.utils.secs
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import java.time.Clock
import java.time.Duration

/**
 * An OSGi wrapper around a [HealthChecker].
 */
abstract class AbstractHealthCheckService @JvmOverloads constructor(
    /**
     * The interval at which health checks will be performed.
     */
    interval: Duration = 10.secs,
    /**
     * If no health checks have been performed in the last {@code timeout}
     * period, the service is considered unhealthy.
     */
    timeout: Duration = 30.secs,
    /**
     * The maximum duration that a call to {@link #performCheck()} is allowed
     * to take. If a call takes longer, the service is considered unhealthy.
     * <p>
     * Note that if a check never completes, we rely on {@link #timeout} instead.
     */
    maxCheckDuration: Duration = 3.secs,
    /**
     * If set, a single health check failure after the initial
     * {@link #STICKY_FAILURES_GRACE_PERIOD} will be result in the service
     * being permanently unhealthy.
     */
    stickyFailures: Boolean = false,
    /**
     * Failures in this period (since the start of the service) are not sticky.
     */
    stickyFailuresGracePeriod: Duration = stickyFailuresGracePeriodDefault,
    clock: Clock = Clock.systemUTC()
) : BundleActivator {
    private val healthCheck = HealthChecker(
        interval,
        timeout,
        maxCheckDuration,
        stickyFailures,
        stickyFailuresGracePeriod,
        healthCheckFunc = ::performCheck,
        clock = clock
    )
    @Throws(Exception::class)
    override fun start(bundleContext: BundleContext) {
        bundleContext.registerService(HealthCheckService::class.java, healthCheck, null)
        healthCheck.start()
    }

    @Throws(Exception::class)
    override fun stop(bundleContext: BundleContext) {
    }

    /**
     * Performs a check to determine whether this service is healthy.
     * This is executed periodically in {@link #executor} since it may block.
     *
     * @throws Exception if the service is not healthy.
     */
    @Throws(Exception::class)
    protected abstract fun performCheck()

    /**
     * Performs a health check and updates this instance's state. Runs
     * periodically in {@link #executor}.
     */

    companion object {
        val stickyFailuresGracePeriodDefault = Duration.ofMinutes(5)
    }
}
