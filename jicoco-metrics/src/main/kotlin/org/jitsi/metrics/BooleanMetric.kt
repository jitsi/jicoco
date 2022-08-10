/*
 * Copyright @ 2022 - present 8x8, Inc.
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
package org.jitsi.metrics

import io.prometheus.client.CollectorRegistry
import io.prometheus.client.Gauge

/**
 * A metric that represents booleans using Prometheus [Gauges][Gauge].
 * A non-zero value corresponds to `true`, zero corresponds to `false`.
 */
class BooleanMetric @JvmOverloads constructor(
    /** the name of this metric */
    override val name: String,
    /** the description of this metric */
    help: String,
    /** the namespace (prefix) of this metric */
    namespace: String,
    /** an optional initial value for this metric */
    internal val initialValue: Boolean = false
) : Metric<Boolean>() {
    private val gauge =
        Gauge.build(name, help).namespace(namespace).create().apply { set(if (initialValue) 1.0 else 0.0) }

    override fun get() = gauge.get() != 0.0

    override fun reset() = set(initialValue)

    override fun register(registry: CollectorRegistry) = this.also { registry.register(gauge) }

    /**
     * Atomically sets the gauge to the given value.
     */
    fun set(newValue: Boolean): Unit = synchronized(gauge) { gauge.set(if (newValue) 1.0 else 0.0) }

    /**
     * Atomically sets the gauge to the given value, returning the updated value.
     *
     * @return the updated value
     */
    fun setAndGet(newValue: Boolean): Boolean = synchronized(gauge) {
        gauge.set(if (newValue) 1.0 else 0.0)
        return newValue
    }
}
