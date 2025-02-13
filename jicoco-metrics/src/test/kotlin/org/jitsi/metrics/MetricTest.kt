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
import io.kotest.matchers.shouldBe

class MetricTest : ShouldSpec() {
    private val namespace = "test"

    init {
        context("Creating any metric type") {
            context("with an empty name") {
                should("throw an exception") {
                    shouldThrow<IllegalStateException> { BooleanMetric("", "Help", namespace) }
                    shouldThrow<IllegalStateException> { CounterMetric("", "Help", namespace) }
                    shouldThrow<IllegalStateException> { DoubleGaugeMetric("", "Help", namespace) }
                    shouldThrow<IllegalStateException> { InfoMetric("", "Help", namespace, "val") }
                    shouldThrow<IllegalStateException> { LongGaugeMetric("", "Help", namespace) }
                }
            }
            context("with an empty help string") {
                should("throw an exception") {
                    shouldThrow<IllegalStateException> { BooleanMetric("name", "", namespace) }
                    shouldThrow<IllegalStateException> { CounterMetric("name", "", namespace) }
                    shouldThrow<IllegalStateException> { DoubleGaugeMetric("name", "", namespace) }
                    shouldThrow<IllegalStateException> { InfoMetric("name", "", namespace, "val") }
                    shouldThrow<IllegalStateException> { LongGaugeMetric("name", "", namespace) }
                }
            }
        }
        context("Creating a BooleanMetric") {
            context("with the default initial value") {
                with(BooleanMetric("testBoolean", "Help", namespace)) {
                    context("and affecting its value") {
                        should("return the correct value") {
                            get() shouldBe false
                            setAndGet(true) shouldBe true
                            set(false).also { get() shouldBe false }
                        }
                    }
                }
            }
            context("with an initial value of true") {
                with(BooleanMetric("testBoolean", "Help", namespace, true)) {
                    should("return true") { get() shouldBe true }
                }
            }
        }
        context("Creating a CounterMetric") {
            context("with the default initial value") {
                with(CounterMetric("testCounter", "Help", namespace)) {
                    context("and incrementing its value repeatedly") {
                        should("return the correct value") {
                            get() shouldBe 0
                            incAndGet() shouldBe 1
                            repeat(20) { inc() }
                            get() shouldBe 21
                            supportsJson shouldBe true
                        }
                    }
                    context("and decrementing its value") {
                        should("throw an exception") {
                            shouldThrow<IllegalArgumentException> { addAndGet(-1) }
                        }
                    }
                }
            }
            context("with a positive initial value") {
                val initialValue: Long = 50
                with(CounterMetric("testCounter", "Help", namespace, initialValue)) {
                    should("return the initial value") { get() shouldBe initialValue }
                }
            }
            context("with a negative initial value") {
                val initialValue: Long = -50
                should("throw an exception") {
                    shouldThrow<IllegalArgumentException> {
                        CounterMetric("testCounter", "Help", namespace, initialValue)
                    }
                }
            }
            context("With labels") {
                context("With initialValue != 0") {
                    shouldThrow<Exception> {
                        CounterMetric("name", "help", namespace, 1, listOf("l1"))
                    }
                }
                with(CounterMetric("testCounter", "Help", namespace, labelNames = listOf("l1", "l2"))) {
                    supportsJson shouldBe false
                    listOf(
                        { get() },
                        { get(listOf("v1")) },
                        { get(listOf("v1", "v2", "v3")) },
                        { inc() },
                        { inc(listOf("v1")) },
                        { inc(listOf("v1", "v2", "v3")) },
                        { add(3) },
                        { add(3, listOf("v1")) },
                        { add(3, listOf("v1", "v2", "v3")) },
                    ).forEach { block ->
                        shouldThrow<Exception> {
                            block()
                        }
                    }

                    val labels = listOf("A", "A")
                    val labels2 = listOf("A", "B")
                    val labels3 = listOf("B", "B")

                    get(labels) shouldBe 0
                    get(labels2) shouldBe 0
                    get(labels3) shouldBe 0

                    addAndGet(3, labels) shouldBe 3
                    get(labels) shouldBe 3
                    get(labels2) shouldBe 0
                    get(labels3) shouldBe 0

                    inc(labels2)
                    get(labels) shouldBe 3
                    get(labels2) shouldBe 1
                    get(labels3) shouldBe 0

                    incAndGet(labels3) shouldBe 1

                    add(2, labels)
                    get(labels) shouldBe 5
                    get(labels2) shouldBe 1
                    get(labels3) shouldBe 1

                    // _total and _created for 3 sets of labels
                    collect()[0].samples.size shouldBe 6
                    remove(labels2)
                    // Down to two sets of labels
                    collect()[0].samples.size shouldBe 4
                    get(labels) shouldBe 5
                    get(labels2) shouldBe 0
                    get(labels3) shouldBe 1
                    // Even a get() will summon a child
                    collect()[0].samples.size shouldBe 6
                }
            }
        }
        context("Creating a LongGaugeMetric") {
            context("with the default initial value") {
                with(LongGaugeMetric("testLongGauge", "Help", namespace)) {
                    context("and affecting its value repeatedly") {
                        should("return the correct value") {
                            get() shouldBe 0
                            inc().also { dec() }
                            get() shouldBe 0
                            decAndGet() shouldBe -1
                            incAndGet() shouldBe 0
                            addAndGet(50) shouldBe 50
                            set(42).also { get() shouldBe 42 }
                        }
                    }
                }
            }
            context("with a given initial value") {
                val initialValue: Long = -50
                with(LongGaugeMetric("testLongGauge", "Help", namespace, initialValue)) {
                    should("return the initial value") { get() shouldBe initialValue }
                }
            }
        }
        context("Creating an InfoMetric") {
            context("with a value different from its name") {
                val value = "testInfoValue"
                with(InfoMetric("testInfo", "Help", namespace, value)) {
                    should("return the correct value") { get() shouldBe value }
                }
            }
        }
        context("HistogramMetric") {
            val namespace = "namespace"
            val name = "histogram_test"
            val fullName = "${namespace}_$name"
            val histogramMetric = HistogramMetric(name, "h", namespace, 1.0, 10.0)
            var count = 0
            var sum = 0.0

            repeat(10) {
                histogramMetric.histogram.observe(1.5)
                sum += 1.5
                count++
            }
            repeat(5) {
                histogramMetric.histogram.observe(25.0)
                sum += 25.0
                count++
            }

            var samples = histogramMetric.histogram.collect()[0].samples
            samples.filter { it.name == "${fullName}_count" }.apply {
                size shouldBe 1
                this[0].value shouldBe count
            }
            samples.filter { it.name == "${fullName}_sum" }.apply {
                size shouldBe 1
                this[0].value shouldBe sum
            }

            histogramMetric.reset()
            samples = histogramMetric.histogram.collect()[0].samples
            samples.filter { it.name == "${fullName}_count" }.apply {
                size shouldBe 1
                this[0].value shouldBe 0
            }
            samples.filter { it.name == "${fullName}_sum" }.apply {
                size shouldBe 1
                this[0].value shouldBe 0
            }
        }
    }
}
