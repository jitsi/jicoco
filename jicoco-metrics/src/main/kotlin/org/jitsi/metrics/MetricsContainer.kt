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
import io.prometheus.client.exporter.common.TextFormat
import org.jitsi.utils.logging2.createLogger
import org.json.simple.JSONObject
import java.io.IOException
import java.io.StringWriter

/**
 * `MetricsContainer` gathers and exports metrics from Jitsi components.
 */
open class MetricsContainer @JvmOverloads constructor(
    /** the registry used to register metrics */
    val registry: CollectorRegistry = CollectorRegistry.defaultRegistry,
    /** Namespace prefix added to all metrics. */
    val namespace: String = "jitsi"
) {
    private val logger = createLogger()

    /**
     * Defines the behavior when registering a metric with a name in use by an existing metric.
     * Useful for testing. Defaults to `true`.
     *
     * If `true`, throws an exception if a metric with a given name already exists.
     * If `false`, attempts to return the existing metric, throwing an exception if there is a type mismatch.
     */
    var checkForNameConflicts = true

    /**
     * Map metric names to wrapped Prometheus metric types using the [Metric] interface.
     */
    private val metrics = mutableMapOf<String, Metric<*>>()

    /**
     * Returns the metrics in this instance as a JSON string.
     *
     * @return a JSON string of the metrics in this instance
     */
    open val jsonString: String
        get() = JSONObject(metrics.mapValues { it.value.get() }).toJSONString()

    /**
     * Returns the metrics in this instance in the Prometheus text-based format.
     * See [Formats](https://github.com/prometheus/docs/blob/main/content/docs/instrumenting/exposition_formats.md).
     *
     * @param contentType the Content-Type header string
     * @return the metrics in this instance in the Prometheus text-based format
     */
    open fun getPrometheusMetrics(contentType: String): String {
        val writer = StringWriter()
        try {
            TextFormat.writeFormat(contentType, writer, registry.metricFamilySamples())
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return writer.toString()
    }

    /**
     * Gets metrics in a format based on the `Accept` header. Returns the content type as the second element of the
     * pair. Defaults to OpenMetrics.
     */
    fun getMetrics(
        /** The HTTP `Accept` header, if present. */
        accept: String?
    ): Pair<String, String> {
        return when {
            accept?.startsWith("application/json") == true -> jsonString to "application/json"
            accept?.startsWith("text/plain") == true ->
                getPrometheusMetrics(TextFormat.CONTENT_TYPE_004) to TextFormat.CONTENT_TYPE_004
            accept?.startsWith("application/openmetrics-text") == true ->
                getPrometheusMetrics(TextFormat.CONTENT_TYPE_OPENMETRICS_100) to TextFormat.CONTENT_TYPE_OPENMETRICS_100
            else ->
                getPrometheusMetrics(TextFormat.CONTENT_TYPE_OPENMETRICS_100) to TextFormat.CONTENT_TYPE_OPENMETRICS_100
        }
    }

    /**
     * Creates and registers a [BooleanMetric] with the given [name], [help] string and optional [initialValue].
     *
     * Throws an exception if a metric with the same name but a different type exists.
     */
    @JvmOverloads
    fun registerBooleanMetric(
        /** the name of the metric */
        name: String,
        /** the description of the metric */
        help: String,
        /** the optional initial value of the metric */
        initialValue: Boolean = false
    ): BooleanMetric {
        if (metrics.containsKey(name)) {
            if (checkForNameConflicts) {
                throw RuntimeException("Could not register metric '$name'. A metric with that name already exists.")
            }
            return metrics[name] as BooleanMetric
        }
        return BooleanMetric(name, help, namespace, initialValue).apply { metrics[name] = register(registry) }
    }

    /**
     * Creates and registers a [CounterMetric] with the given [name], [help] string and optional [initialValue].
     * If omitted, a "_total" suffix is added to the metric name to ensure consistent (Counter) metric naming.
     *
     * Throws an exception if a metric with the same name but a different type exists.
     */
    @JvmOverloads
    fun registerCounter(
        /** the name of the metric */
        name: String,
        /** the description of the metric */
        help: String,
        /** the optional initial value of the metric */
        initialValue: Long = 0
    ): CounterMetric {
        val newName = if (name.endsWith("_total")) {
            name
        } else {
            "${name}_total".also {
                logger.debug("Counter '$name' was renamed to '$it' to ensure consistent metric naming.")
            }
        }
        if (metrics.containsKey(newName) or metrics.containsKey(name)) {
            if (checkForNameConflicts) {
                throw RuntimeException("Could not register metric '$newName'. A metric with that name already exists.")
            }
            return metrics[newName] as CounterMetric
        }
        return CounterMetric(newName, help, namespace, initialValue).apply { metrics[newName] = register(registry) }
    }

    /**
     * Creates and registers a [LongGaugeMetric] with the given [name], [help] string and optional [initialValue].
     *
     * Throws an exception if a metric with the same name but a different type exists.
     */
    @JvmOverloads
    fun registerLongGauge(
        /** the name of the metric */
        name: String,
        /** the description of the metric */
        help: String,
        /** the optional initial value of the metric */
        initialValue: Long = 0
    ): LongGaugeMetric {
        if (metrics.containsKey(name)) {
            if (checkForNameConflicts) {
                throw RuntimeException("Could not register metric '$name'. A metric with that name already exists.")
            }
            return metrics[name] as LongGaugeMetric
        }
        return LongGaugeMetric(name, help, namespace, initialValue).apply { metrics[name] = register(registry) }
    }

    /**
     * Creates and registers a [DoubleGaugeMetric] with the given [name], [help] string and optional [initialValue].
     *
     * Throws an exception if a metric with the same name but a different type exists.
     */
    @JvmOverloads
    fun registerDoubleGauge(
        /** the name of the metric */
        name: String,
        /** the description of the metric */
        help: String,
        /** the optional initial value of the metric */
        initialValue: Double = 0.0
    ): DoubleGaugeMetric {
        if (metrics.containsKey(name)) {
            if (checkForNameConflicts) {
                throw RuntimeException("Could not register metric '$name'. A metric with that name already exists.")
            }
            return metrics[name] as DoubleGaugeMetric
        }
        return DoubleGaugeMetric(name, help, namespace, initialValue).apply { metrics[name] = register(registry) }
    }

    /**
     * Creates and registers an [InfoMetric] with the given [name], [help] string and [value].
     *
     * Throws an exception if a metric with the same name but a different type exists.
     */
    fun registerInfo(
        /** the name of the metric */
        name: String,
        /** the description of the metric */
        help: String,
        /** the value of the metric */
        value: String
    ): InfoMetric {
        if (metrics.containsKey(name)) {
            if (checkForNameConflicts) {
                throw RuntimeException("Could not register metric '$name'. A metric with that name already exists.")
            }
            return metrics[name] as InfoMetric
        }
        return InfoMetric(name, help, namespace, value).apply { metrics[name] = register(registry) }
    }

    fun registerHistogram(
        /** the name of the metric */
        name: String,
        /** the description of the metric */
        help: String,
        vararg buckets: Double
    ): HistogramMetric {
        if (metrics.containsKey(name)) {
            if (checkForNameConflicts) {
                throw RuntimeException("Could not register metric '$name'. A metric with that name already exists.")
            }
            return metrics[name] as HistogramMetric
        }

        return HistogramMetric(name, help, namespace, *buckets).apply { metrics[name] = register(registry) }
    }

    /**
     * Resets all metrics in this container to their default values.
     */
    fun resetAll() {
        metrics.values.forEach { it.reset() }
    }
}
