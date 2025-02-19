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
import io.prometheus.client.Counter

/**
 * A long metric wrapper for a Prometheus [Counter], which is monotonically increasing.
 * Provides atomic operations such as [incAndGet].
 *
 * @see [Prometheus Counter](https://prometheus.io/docs/concepts/metric_types/.counter)
 *
 * @see [Prometheus Gauge](https://prometheus.io/docs/concepts/metric_types/.gauge)
 */
class CounterMetric @JvmOverloads constructor(
    /** the name of this metric */
    override val name: String,
    /** the description of this metric */
    help: String,
    /** the namespace (prefix) of this metric */
    namespace: String,
    /** an optional initial value for this metric */
    internal val initialValue: Long = 0L,
    /** Label names for this metric. If non-empty, the initial value must be 0 and all get/update calls MUST
     * specify values for the labels. Calls to simply [get()] or [inc()] will fail with an exception. */
    val labelNames: List<String> = emptyList()
) : Metric<Long>() {
    private val counter = run {
        val builder = Counter.build(name, help).namespace(namespace)
        if (labelNames.isNotEmpty()) {
            builder.labelNames(*labelNames.toTypedArray())
            if (initialValue != 0L) {
                throw IllegalArgumentException("Cannot set an initial value for a labeled counter")
            }
        }
        builder.create().apply {
            if (initialValue != 0L) {
                inc(initialValue.toDouble())
            }
        }
    }

    /** When we have labels [get()] throws an exception and the JSON format is not supported. */
    override val supportsJson: Boolean = labelNames.isEmpty()

    override fun get() = counter.get().toLong()
    fun get(labels: List<String>) = counter.labels(*labels.toTypedArray()).get().toLong()

    override fun reset() {
        synchronized(counter) {
            counter.apply {
                clear()
                if (initialValue != 0L) {
                    inc(initialValue.toDouble())
                }
            }
        }
    }

    override fun register(registry: CollectorRegistry) = this.also { registry.register(counter) }

    /**
     * Atomically adds the given value to this counter.
     */
    @JvmOverloads
    fun add(delta: Long, labels: List<String> = emptyList()) = synchronized(counter) {
        if (labels.isEmpty()) {
            counter.inc(delta.toDouble())
        } else {
            counter.labels(*labels.toTypedArray()).inc(delta.toDouble())
        }
    }

    /**
     * Atomically adds the given value to this counter, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun addAndGet(delta: Long, labels: List<String> = emptyList()): Long = synchronized(counter) {
        return if (labels.isEmpty()) {
            counter.inc(delta.toDouble())
            counter.get().toLong()
        } else {
            counter.labels(*labels.toTypedArray()).inc(delta.toDouble())
            counter.labels(*labels.toTypedArray()).get().toLong()
        }
    }

    /**
     * Atomically increments the value of this counter by one, returning the updated value.
     *
     * @return the updated value
     */
    @JvmOverloads
    fun incAndGet(labels: List<String> = emptyList()) = addAndGet(1, labels)

    /**
     * Atomically increments the value of this counter by one.
     */
    @JvmOverloads
    fun inc(labels: List<String> = emptyList()) = synchronized(counter) {
        if (labels.isEmpty()) {
            counter.inc()
        } else {
            counter.labels(*labels.toTypedArray()).inc()
        }
    }

    /** Remove the child with the given labels (the metric with those labels will stop being emitted) */
    fun remove(labels: List<String> = emptyList()) = synchronized(counter) {
        if (labels.isNotEmpty()) {
            counter.remove(*labels.toTypedArray())
        }
    }

    internal fun collect() = counter.collect()
}
