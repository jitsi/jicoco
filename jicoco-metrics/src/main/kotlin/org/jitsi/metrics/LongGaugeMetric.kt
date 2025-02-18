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
 * A long metric wrapper for Prometheus [Gauges][Gauge].
 * Provides atomic operations such as [incAndGet].
 *
 * @see [Prometheus Gauge](https://prometheus.io/docs/concepts/metric_types/.gauge)
 */
class LongGaugeMetric @JvmOverloads constructor(
    /** the name of this metric */
    override val name: String,
    /** the description of this metric */
    help: String,
    /** the namespace (prefix) of this metric */
    namespace: String,
    /** an optional initial value for this metric */
    internal val initialValue: Long = 0L,
    /** Label names for this metric. If non-empty, the initial value must be 0 and all get/update calls MUST
     * specify values for the labels. Calls to simply get() or set() will fail with an exception. */
    val labelNames: List<String> = emptyList()
) : Metric<Long>() {
    private val gauge = run {
        val builder = Gauge.build(name, help).namespace(namespace)
        if (labelNames.isNotEmpty()) {
            builder.labelNames(*labelNames.toTypedArray())
            if (initialValue != 0L) {
                throw IllegalArgumentException("Cannot set an initial value for a labeled gauge")
            }
        }
        builder.create().apply {
            if (initialValue != 0L) {
                set(initialValue.toDouble())
            }
        }
    }

    /** When we have labels [get()] throws an exception and the JSON format is not supported. */
    override val supportsJson: Boolean = labelNames.isEmpty()
    override fun get() = gauge.get().toLong()
    fun get(labels: List<String>) = gauge.labels(*labels.toTypedArray()).get().toLong()

    override fun reset() = synchronized(gauge) {
        gauge.clear()
        if (initialValue != 0L) {
            gauge.inc(initialValue.toDouble())
        }
    }

    override fun register(registry: CollectorRegistry) = this.also { registry.register(gauge) }

    /**
     * Atomically sets the gauge to the given value.
     */
    fun set(newValue: Long, labels: List<String> = emptyList()): Unit = synchronized(gauge) {
        if (labels.isEmpty()) {
            gauge.set(newValue.toDouble())
        } else {
            gauge.labels(*labels.toTypedArray()).set(newValue.toDouble())
        }
    }

    /**
     * Atomically increments the value of this gauge by one.
     */
    fun inc(labels: List<String> = emptyList()) = synchronized(gauge) {
        if (labels.isEmpty()) {
            gauge.inc()
        } else {
            gauge.labels(*labels.toTypedArray()).inc()
        }
    }

    /**
     * Atomically decrements the value of this gauge by one.
     */
    fun dec(labels: List<String> = emptyList()) = synchronized(gauge) {
        if (labels.isEmpty()) {
            gauge.dec()
        } else {
            gauge.labels(*labels.toTypedArray()).dec()
        }
    }

    /**
     * Atomically adds the given value to this gauge, returning the updated value.
     *
     * @return the updated value
     */
    fun addAndGet(delta: Long, labels: List<String> = emptyList()): Long = synchronized(gauge) {
        return if (labels.isEmpty()) {
            gauge.inc(delta.toDouble())
            gauge.get().toLong()
        } else {
            with(gauge.labels(*labels.toTypedArray())) {
                inc(delta.toDouble())
                get().toLong()
            }
        }
    }

    /**
     * Atomically increments the value of this gauge by one, returning the updated value.
     *
     * @return the updated value
     */
    fun incAndGet(labels: List<String> = emptyList()) = addAndGet(1, labels)

    /**
     * Atomically decrements the value of this gauge by one, returning the updated value.
     *
     * @return the updated value
     */
    fun decAndGet(labels: List<String> = emptyList()) = addAndGet(-1, labels)

    /** Remove the child with the given labels (the metric with those labels will stop being emitted) */
    fun remove(labels: List<String> = emptyList()) = synchronized(gauge) {
        if (labels.isNotEmpty()) {
            gauge.remove(*labels.toTypedArray())
        }
    }
    internal fun collect() = gauge.collect()
}
