/*
 * Copyright @ 2023 - present 8x8, Inc.
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
import io.prometheus.client.Histogram
import org.json.simple.JSONObject

class HistogramMetric(
    override val name: String,
    /** the description of this metric */
    private val help: String,
    /** the namespace (prefix) of this metric */
    val namespace: String,
    vararg buckets: Double
) : Metric<JSONObject>() {
    val histogram: Histogram = Histogram.build(name, help).namespace(namespace).buckets(*buckets).create()

    override fun get(): JSONObject = JSONObject().apply {
        histogram.collect().forEach {
            it.samples.forEach { sample ->
                if (sample.name.startsWith("${namespace}_${name}_")) {
                    val shortName = sample.name.substring("${namespace}_${name}_".length)
                    if (shortName == "bucket" && sample.labelNames.size == 1) {
                        put("${shortName}_${sample.labelNames[0]}_${sample.labelValues[0]}", sample.value)
                    } else {
                        put(shortName, sample.value)
                    }
                }
            }
        }
    }

    override fun reset() = histogram.clear()

    override fun register(registry: CollectorRegistry): Metric<JSONObject> = this.also { registry.register(histogram) }
}
