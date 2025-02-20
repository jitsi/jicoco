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
 * A double metric wrapper for Prometheus [Gauges][Gauge].
 * Provides atomic operations such as [incAndGet].
 *
 * @see [Prometheus Gauge](https://prometheus.io/docs/concepts/metric_types/.gauge)
 */
class DoubleGaugeMetric @JvmOverloads constructor(
    /** the name of this metric */
    override val name: String,
    /** the description of this metric */
    help: String,
    /** the namespace (prefix) of this metric */
    namespace: String,
    /** an optional initial value for this metric */
    internal val initialValue: Double = 0.0,
    /** Label names for this metric. If non-empty, the initial value must be 0 and all get/update calls MUST
     * specify values for the labels. Calls to simply [get()] or [set(Double)] will fail with an exception. */
    val labelNames: List<String> = emptyList()
) : Metric<Double>() {
    private val gauge = run {
        val builder = Gauge.build(name, help).namespace(namespace)
        if (labelNames.isNotEmpty()) {
            builder.labelNames(*labelNames.toTypedArray())
            if (initialValue != 0.0) {
                throw IllegalArgumentException("Cannot set an initial value for a labeled gauge")
            }
        }
        builder.create().apply {
            if (initialValue != 0.0) {
                set(initialValue)
            }
        }
    }

    /** When we have labels [get()] throws an exception and the JSON format is not supported. */
    override val supportsJson: Boolean = labelNames.isEmpty()

    override fun get() = gauge.get()
    fun get(labelNames: List<String>) = gauge.labels(*labelNames.toTypedArray()).get()

    override fun reset() = synchronized(gauge) {
        gauge.clear()
        if (initialValue != 0.0) {
            gauge.set(initialValue)
        }
    }

    override fun register(registry: CollectorRegistry) = this.also { registry.register(gauge) }

    /**
     * Sets the value of this gauge to the given value.
     */
    @JvmOverloads
    fun set(newValue: Double, labels: List<String> = emptyList()) {
        if (labels.isEmpty()) {
            gauge.set(newValue)
        } else {
            gauge.labels(*labels.toTypedArray()).set(newValue)
        }
    }

    /**
     * Atomically sets the gauge to the given value, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun setAndGet(newValue: Double, labels: List<String> = emptyList()): Double = synchronized(gauge) {
        return if (labels.isEmpty()) {
            gauge.set(newValue)
            gauge.get()
        } else {
            with(gauge.labels(*labels.toTypedArray())) {
                set(newValue)
                get()
            }
        }
    }

    /**
     * Atomically adds the given value to this gauge, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun addAndGet(delta: Double, labels: List<String> = emptyList()): Double = synchronized(gauge) {
        return if (labels.isEmpty()) {
            gauge.inc(delta)
            gauge.get()
        } else {
            with(gauge.labels(*labels.toTypedArray())) {
                inc(delta)
                get()
            }
        }
    }

    /**
     * Atomically increments the value of this gauge by one, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun incAndGet(labels: List<String> = emptyList()) = addAndGet(1.0, labels)

    /**
     * Atomically decrements the value of this gauge by one, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun decAndGet(labels: List<String> = emptyList()) = addAndGet(-1.0, labels)

    /** Remove the child with the given labels (the metric with those labels will stop being emitted) */
    fun remove(labels: List<String> = emptyList()) = synchronized(gauge) {
        if (labels.isNotEmpty()) {
            gauge.remove(*labels.toTypedArray())
        }
    }

    internal fun collect() = gauge.collect()
}
