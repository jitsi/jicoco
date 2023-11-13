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

class HistogramMetric(
    override val name: String,
    /** the description of this metric */
    private val help: String,
    /** the namespace (prefix) of this metric */
    namespace: String,
    vararg buckets: Double
) : Metric<String>() {
    val histogram: Histogram = Histogram.build(name, help).namespace(namespace).buckets(*buckets).create()

    override fun get(): String = "Histogram for $help. Rendering to JSON not supported, use openmetrics format."

    // TODO. I don't think Histogram supports a set/reset.
    override fun reset() {}

    override fun register(registry: CollectorRegistry): Metric<String> = this.also { registry.register(histogram) }

    override val supportsJson = false
}
