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

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat

class MetricsContainerTest : ShouldSpec() {

    private val mc = MetricsContainer()
    private val otherRegistry = CollectorRegistry()

    init {
        context("Registering metrics") {
            val booleanMetric = mc.registerBooleanMetric("boolean", "A boolean metric")
            val counter = mc.registerCounter("counter", "A counter metric")
            val info = mc.registerInfo("info", "An info metric", "value")
            val longGauge = mc.registerLongGauge("gauge", "A gauge metric")

            context("twice in the same registry") {
                context("while checking for name conflicts") {
                    should("throw a RuntimeException") {
                        shouldThrow<RuntimeException> { mc.registerBooleanMetric("boolean", "A boolean metric") }
                        // "counter" is renamed to "counter_total" so both should throw an exception
                        shouldThrow<RuntimeException> { mc.registerCounter("counter", "A counter metric") }
                        shouldThrow<RuntimeException> { mc.registerCounter("counter_total", "A counter metric") }
                        // we test this because the Prometheus JVM library stores Counters without the "_total" suffix
                        shouldThrow<RuntimeException> { mc.registerCounter("boolean_total", "A counter metric") }
                    }
                }
                context("without checking for name conflicts") {
                    mc.checkForNameConflicts = false
                    should("return an existing metric") {
                        booleanMetric shouldBe mc.registerBooleanMetric("boolean", "A boolean metric")
                        // "counter" is renamed to "counter_total" so both should return the same metric
                        counter shouldBe mc.registerCounter("counter", "A counter metric")
                        counter shouldBe mc.registerCounter("counter_total", "A counter metric")
                        info shouldBe mc.registerInfo("info", "An info metric", "value")
                        longGauge shouldBe mc.registerLongGauge("gauge", "A gauge metric")
                    }
                    mc.checkForNameConflicts = true
                }
            }
            context("in a new registry") {
                should("successfully register metrics") {
                    booleanMetric.register(otherRegistry)
                    counter.register(otherRegistry)
                    info.register(otherRegistry)
                    longGauge.register(otherRegistry)
                }
                should("contain the same metrics in both registries") {
                    val a = CollectorRegistry.defaultRegistry.metricFamilySamples().toList()
                    val b = otherRegistry.metricFamilySamples().toList()
                    a shouldContainExactly b
                }
            }
            context("and altering their values") {
                booleanMetric.set(!booleanMetric.get())
                counter.add(5)
                longGauge.set(5)
                context("then resetting all metrics in the MetricsContainer") {
                    mc.resetAll()
                    should("set all metric values to their initial values") {
                        booleanMetric.get() shouldBe booleanMetric.initialValue
                        counter.get() shouldBe counter.initialValue
                        longGauge.get() shouldBe longGauge.initialValue
                    }
                }
            }
        }
        context("Getting metrics with different accepted content types") {
            should("return the correct content type") {
                mc.getMetrics(emptyList()).second shouldBe TextFormat.CONTENT_TYPE_OPENMETRICS_100
                mc.getMetrics(listOf("text/plain")).second shouldBe TextFormat.CONTENT_TYPE_004
                mc.getMetrics(listOf("application/json")).second shouldBe "application/json"
                mc.getMetrics(listOf("application/openmetrics-text")).second shouldBe
                    TextFormat.CONTENT_TYPE_OPENMETRICS_100
                mc.getMetrics(listOf("application/openmetrics-text", "application/json")).second shouldBe
                    TextFormat.CONTENT_TYPE_OPENMETRICS_100
                mc.getMetrics(listOf("application/json", "application/openmetrics-text")).second shouldBe
                    "application/json"
                mc.getMetrics(
                    listOf(
                        "application/json",
                        "application/other",
                        "application/openmetrics-text"
                    )
                ).second shouldBe
                    "application/json"
                mc.getMetrics(listOf("application/json", "*/*", "application/openmetrics-text")).second shouldBe
                    "application/json"
                mc.getMetrics(listOf("*/*", "application/json", "*/*", "application/openmetrics-text")).second shouldBe
                    TextFormat.CONTENT_TYPE_OPENMETRICS_100
                shouldThrow<MetricsContainer.NoSupportedMediaTypeException> {
                    mc.getMetrics(listOf("application/something", "application/something-else"))
                }
            }
        }
    }
}
