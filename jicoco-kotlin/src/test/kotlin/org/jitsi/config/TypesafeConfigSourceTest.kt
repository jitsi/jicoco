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

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.AbstractShouldSpec
import io.kotlintest.specs.ShouldSpec
import org.jitsi.metaconfig.ConfigSource
import java.time.Duration
import kotlin.reflect.typeOf

class TypesafeConfigSourceTest : ShouldSpec() {

    init {
        "Retrieving a value of type" {
            "Boolean" {
                withConfig { "boolean=true" }
                getValue<Boolean>("boolean") shouldBe true
            }
            "Int" {
                withConfig { "int=42" }
                getValue<Int>("int") shouldBe 42
            }
            "Long" {
                withConfig { "long=42" }
                getValue<Long>("long") shouldBe 42L
            }
            "Double" {
                withConfig { "double=42.5" }
                getValue<Double>("double") shouldBe 42.5
            }
            "String" {
                withConfig { "string=\"hello, world\"" }
                getValue<String>("string") shouldBe "hello, world"
            }
            "List<String>" {
                withConfig {
                    "strings = [ \"one\", \"two\", \"three\" ]"
                }
                getValue<List<String>>("strings") shouldBe listOf("one", "two", "three")
            }
            "List<Int>>" {
                withConfig {
                    "ints = [ 41, 42, 43 ]"
                }
                getValue<List<Int>>("ints") shouldBe listOf(41, 42, 43)
            }
            "Duration" {
                withConfig { "duration = 1 minute" }
                getValue<Duration>("duration") shouldBe Duration.ofMinutes(1)
            }
            "ConfigObject" {
                withConfig {
                    """
                        obj = {
                            num = 42
                            str = "hello"
                        }
                    """.trimIndent()
                }
                val obj = getValue<ConfigObject>("obj")
                obj["num"]!!.unwrapped() as Number shouldBe 42
                obj["str"]!!.unwrapped() as String shouldBe "hello"
            }
            "List<Config>" {
                withConfig {
                    """
                        objs = [
                            {
                                num = 42
                                str = "hello"
                            },
                            {
                                num = 43
                                str = "goodbye"
                            }
                        ]
                    """.trimIndent()
                }
                val objs = getValue<List<Config>>("objs")
                objs shouldHaveSize 2
                objs[0].getInt("num") shouldBe 42
                objs[0].getString("str") shouldBe "hello"
                objs[1].getInt("num") shouldBe 43
                objs[1].getString("str") shouldBe "goodbye"
            }
            "Enum" {
                withConfig {
                    "color=BLUE"
                }
                getValue<Color>("color") shouldBe Color.BLUE
            }
        }
    }
}

private fun AbstractShouldSpec.ShouldScope.withConfig(block: () -> String) {
    val config = TypesafeConfigSource("testConfig", ConfigFactory.parseString(block()))
    this.context.putMetaData("config", config)
}

private inline fun <reified T : Any> AbstractShouldSpec.ShouldScope.getValue(path: String): T {
    val config = this.context.metaData()["config"] as ConfigSource
    val getter = config.getterFor(typeOf<T>())
    return getter(path) as T
}

private enum class Color {
    BLUE
}
