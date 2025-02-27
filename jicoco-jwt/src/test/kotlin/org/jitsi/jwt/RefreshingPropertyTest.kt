/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.jwt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.jitsi.utils.time.FakeClock
import java.time.Duration

class RefreshingPropertyTest : ShouldSpec({
    val clock = FakeClock()

    context("A refreshing property") {
        val obj = object {
            private var generation = 0
            val prop: Int? by RefreshingProperty(Duration.ofSeconds(1), clock) {
                println("Refreshing, generation was $generation")
                generation++
            }
        }
        should("return the right initial value") {
            obj.prop shouldBe 0
        }
        context("after the timeout has elapsed") {
            clock.elapse(Duration.ofSeconds(1))
            should("refresh after the timeout has elapsed") {
                obj.prop shouldBe 1
            }
            should("not refresh again") {
                obj.prop shouldBe 1
            }
            context("and then a long amount of time passes") {
                clock.elapse(Duration.ofMinutes(30))
                should("refresh again") {
                    obj.prop shouldBe 2
                }
            }
        }
        context("whose creator function throws an exception") {
            val exObj = object {
                val prop: Int? by RefreshingProperty<Int>(Duration.ofSeconds(1), clock) {
                    throw Exception("boom")
                }
            }
            should("return null") {
                exObj.prop shouldBe null
            }
        }
        context("whose creator function throws an Error") {
            val exObj = object {
                val prop: Int? by RefreshingProperty<Int>(Duration.ofSeconds(1), clock) {
                    throw NoClassDefFoundError("javax.xml.bind.DatatypeConverter")
                }
            }
            val error = shouldThrow<NoClassDefFoundError> {
                println(exObj.prop)
            }
            error.message shouldContain "javax.xml.bind.DatatypeConverter"
        }
    }
})
