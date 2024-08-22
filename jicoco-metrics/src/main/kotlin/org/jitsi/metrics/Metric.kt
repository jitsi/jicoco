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
import io.prometheus.client.SimpleCollector

/**
 * `Metric` provides methods common to all Prometheus metric type wrappers.
 *
 * A wrapper that extends `Metric<T>` produces and consumes values of type `T`.
 * Metrics are held in a [MetricsContainer].
 */
sealed class Metric<T> {

    /**
     * The name of this metric.
     */
    abstract val name: String

    /**
     * Supplies the current value of this metric.
     */
    abstract fun get(): T

    /**
     * Resets the value of this metric to its initial value.
     */
    internal abstract fun reset()

    /**
     * Registers this metric with the given [CollectorRegistry] and returns it.
     */
    internal abstract fun register(registry: CollectorRegistry): Metric<T>

    /**
     * Sets the OpenMetrics format unit of this metric from its name (suffix delimited by an underscore).
     *
     * See: [OpenMetrics](https://github.com/OpenObservability/OpenMetrics/blob/main/specification/OpenMetrics.md#unit)
     */
    internal fun SimpleCollector.Builder<*, *>.setUnit() {
        val suffix = name.substringAfterLast("_", "")
        if (UNITS.contains(suffix)) {
            unit(suffix)
        }
    }
}

private val UNITS = setOf("milliseconds", "seconds", "bits", "kilobits", "megabits", "bytes", "kilobytes", "megabytes")
