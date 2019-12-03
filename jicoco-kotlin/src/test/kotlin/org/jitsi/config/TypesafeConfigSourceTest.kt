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

package org.jitsi.config

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.config.ConfigSource
import java.time.Duration
import java.time.Period
import java.time.temporal.TemporalAmount

class TypesafeConfigSourceTest : ShouldSpec() {

    init {
        "Retrieving a value of type" {
            "Boolean" {
                withConfig {
                    "some.boolean.value=true"
                }
                getValue<Boolean>("some.boolean.value") shouldBe true
            }
            "Number" {
                withConfig {
                    "some.number.value=42"
                }
                getValue<Number>("some.number.value") shouldBe 42
            }
            "Int" {
                withConfig {
                    "some.int.value=42"
                }
                getValue<Int>("some.int.value") shouldBe 42
            }
            "Long" {
                withConfig {
                    "some.long.value=42"
                }
                getValue<Int>("some.long.value") shouldBe 42
            }
            "Double" {
                withConfig {
                    "some.double.value=42.42"
                }
                getValue<Double>("some.double.value") shouldBe 42.42
            }
            "String" {
                withConfig {
                    "some.string.value=\"hello, world\""
                }
                getValue<String>("some.string.value") shouldBe "hello, world"
            }
            "ConfigObject" {
                withConfig {
                    """
                        scope-a {
                            obj {
                                num = 42
                                str = "hello"
                            }
                        }
                    """.trimIndent()
                }
                val obj = getValue<ConfigObject>("scope-a.obj")
                (obj["num"]?.unwrapped() as Number) shouldBe 42
                (obj["str"]?.unwrapped() as String) shouldBe "hello"
            }
            "ConfigValue" {
                withConfig {
                    "some.value=10"
                }
                getValue<ConfigValue>("some.value").unwrapped() shouldBe 10
            }
            "Duration" {
                withConfig {
                    "some.duration.value=500 ms"
                }
                getValue<Duration>("some.duration.value") shouldBe Duration.ofMillis(500)
            }
            "Period" {
                withConfig {
                    "some.period.value=1 day"
                }
                getValue<Period>("some.period.value") shouldBe Period.ofDays(1)
            }
            "TemporalAmount" {
                withConfig {
                    "some.temporalamount.value=500 ms"
                }
                getValue<TemporalAmount>("some.temporalamount.value") shouldBe Duration.ofMillis(500)
            }
        }
    }

    private fun ShouldScope.withConfig(block: () -> String) {
        val config = TypesafeConfigSource("testConfig") { ConfigFactory.parseString(block()) }
        this.context.putMetaData("config", config)
    }

    private inline fun <reified T : Any> ShouldScope.getValue(path: String): T {
        val config = this.context.metaData()["config"] as ConfigSource
        val getter = config.getterFor(T::class)
        return getter(path)
    }
}