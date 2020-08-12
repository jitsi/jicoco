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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jitsi.metaconfig.ConfigException
import org.jitsi.metaconfig.ConfigSource
import java.time.Duration
import java.util.regex.Pattern
import kotlin.reflect.typeOf

class TypesafeConfigSourceTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        context("Retrieving a value of type") {
            context("Boolean") {
                withConfig("boolean=true") {
                    getValue<Boolean>("boolean") shouldBe true
                }
            }
            context("Int") {
                withConfig("int=42") {
                    getValue<Int>("int") shouldBe 42
                }
            }
            context("Long") {
                withConfig("long=42") {
                    getValue<Long>("long") shouldBe 42L
                }
            }
            context("Double") {
                withConfig("double=42.5") {
                    getValue<Double>("double") shouldBe 42.5
                }
            }
            context("String") {
                withConfig("string=\"hello, world\"") {
                    getValue<String>("string") shouldBe "hello, world"
                }
            }
            context("List<String>") {
                withConfig("strings = [ \"one\", \"two\", \"three\" ]") {
                    getValue<List<String>>("strings") shouldBe listOf("one", "two", "three")
                }
            }
            context("List<Int>") {
                withConfig("ints = [ 41, 42, 43 ]") {
                    getValue<List<Int>>("ints") shouldBe listOf(41, 42, 43)
                }
            }
            context("Duration") {
                withConfig("duration = 1 minute") {
                    getValue<Duration>("duration") shouldBe Duration.ofMinutes(1)
                }
            }
            context("ConfigObject") {
                withConfig(
                    """
                    obj = {
                        num = 42
                        str = "hello"
                    }
                    """.trimIndent()
                ) {
                    val obj = getValue<ConfigObject>("obj").toConfig()
                    obj.getInt("num") shouldBe 42
                    obj.getString("str") shouldBe "hello"
                }
            }
            context("List<Config>") {
                withConfig(
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
                ) {
                    val objs = getValue<List<Config>>("objs")
                    objs shouldHaveSize 2
                    objs[0].getInt("num") shouldBe 42
                    objs[0].getString("str") shouldBe "hello"
                    objs[1].getInt("num") shouldBe 43
                    objs[1].getString("str") shouldBe "goodbye"
                }
            }
            context("Enum") {
                withConfig("color=BLUE") {
                    getValue<Color>("color") shouldBe Color.BLUE
                }
            }
            context("Pattern") {
                withConfig("pattern = \"abc\"") {
                    getValue<Pattern>("pattern").pattern() shouldBe "abc"
                }
                context("when the pattern is invalid") {
                    withConfig("pattern = \"(\"") {
                        shouldThrow<ConfigException.UnableToRetrieve.Error> {
                            getValue<Pattern>("pattern").pattern()
                        }
                    }
                }
            }
        }
    }
}

private fun withConfig(configStr: String, block: ConfigScope.() -> Unit) {
    val config = TypesafeConfigSource("testConfig", ConfigFactory.parseString(configStr))
    ConfigScope(config).apply(block)
}

private class ConfigScope(private val config: ConfigSource) {
    inline fun <reified T : Any> getValue(path: String): T {
        val getter = config.getterFor(typeOf<T>())
        return getter(path) as T
    }
}

private enum class Color {
    BLUE
}
