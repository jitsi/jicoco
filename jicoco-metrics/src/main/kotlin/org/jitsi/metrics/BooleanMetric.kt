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
    internal val initialValue: Boolean = false,
    /** Label names for this metric. If non-empty, the initial value must be false and all get/update calls MUST
     * specify values for the labels. Calls to simply get() or set() will fail with an exception. */
    val labelNames: List<String> = emptyList()
) : Metric<Boolean>() {
    private val gauge = run {
        val builder = Gauge.build(name, help).namespace(namespace)
        if (labelNames.isNotEmpty()) {
            builder.labelNames(*labelNames.toTypedArray())
            if (initialValue) {
                throw IllegalArgumentException("Cannot set an initial value for a labeled gauge")
            }
        }
        builder.create().apply {
            if (initialValue) {
                set(1.0)
            }
        }
    }

    override val supportsJson: Boolean = labelNames.isEmpty()
    override fun get() = gauge.get() != 0.0
    fun get(labels: List<String>) = gauge.labels(*labels.toTypedArray()).get() != 0.0

    override fun reset() = synchronized(gauge) {
        gauge.clear()
        if (initialValue) {
            gauge.set(1.0)
        }
    }

    override fun register(registry: CollectorRegistry) = this.also { registry.register(gauge) }

    /**
     * Atomically sets the gauge to the given value.
     */
    @JvmOverloads
    fun set(newValue: Boolean, labels: List<String> = emptyList()): Unit = synchronized(gauge) {
        if (labels.isEmpty()) {
            gauge.set(if (newValue) 1.0 else 0.0)
        } else {
            gauge.labels(*labels.toTypedArray()).set(if (newValue) 1.0 else 0.0)
        }
    }

    /**
     * Atomically sets the gauge to the given value, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun setAndGet(newValue: Boolean, labels: List<String> = emptyList()): Boolean = synchronized(gauge) {
        set(newValue, labels)
        return newValue
    }

    /** Remove the child with the given labels (the metric with those labels will stop being emitted) */
    fun remove(labels: List<String> = emptyList()) = synchronized(gauge) {
        if (labels.isNotEmpty()) {
            gauge.remove(*labels.toTypedArray())
        }
    }
    internal fun collect() = gauge.collect()
}
