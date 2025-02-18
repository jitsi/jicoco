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
import io.prometheus.client.Info

/**
 * `InfoMetric` wraps around a single key-value information pair.
 * Useful for general information such as build versions, JVB region, etc.
 * In the Prometheus exposition format, these are shown as labels of either a custom metric (OpenMetrics)
 * or a [Gauge][io.prometheus.client.Gauge] (0.0.4 plain text).
 */
class InfoMetric @JvmOverloads constructor(
    /** the name of this metric */
    override val name: String,
    /** the description of this metric */
    help: String,
    /** the namespace (prefix) of this metric */
    namespace: String,
    /** the value of this info metric */
    internal val value: String = "",
    /** Label names for this metric */
    val labelNames: List<String> = emptyList()
) : Metric<String>() {
    private val info = run {
        val builder = Info.build(name, help).namespace(namespace)
        if (labelNames.isNotEmpty()) {
            builder.labelNames(*labelNames.toTypedArray())
        }
        builder.create().apply {
            if (labelNames.isEmpty()) {
                info(name, value)
            }
        }
    }

    override fun get() = if (labelNames.isEmpty()) value else throw UnsupportedOperationException()
    fun get(labels: List<String> = emptyList()) =
        if (labels.isEmpty()) value else info.labels(*labels.toTypedArray()).get()[name]

    override fun reset() = if (labelNames.isEmpty()) info.info(name, value) else info.clear()

    override fun register(registry: CollectorRegistry) = this.also { registry.register(info) }

    /** Remove the child with the given labels (the metric with those labels will stop being emitted) */
    fun remove(labels: List<String> = emptyList()) = synchronized(info) {
        if (labels.isNotEmpty()) {
            info.remove(*labels.toTypedArray())
        }
    }

    fun set(labels: List<String>, value: String) {
        if (labels.isNotEmpty()) {
            info.labels(*labels.toTypedArray()).info(name, value)
        }
    }
    internal fun collect() = info.collect()
}
