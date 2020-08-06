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

import io.kotest.core.spec.IsolationMode
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.extensions.system.withSystemProperties
import java.util.Properties

class AbstractReadOnlyConfigurationServiceTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val config = TestReadOnlyConfigurationService()

    init {
        context("retrieving a property") {
            context("present in both the config and the system") {
                config["some.prop"] = "42"
                withSystemProperties("some.prop" to "43") {
                    should("use the config property") {
                        config.getInt("some.prop", 0) shouldBe 42
                    }
                }
            }
            context("not present anywhere") {
                should("return null") {
                    config.getProperty("missing") shouldBe null
                }
            }
        }
        context("retrieving all properties by prefix") {
            config["a.b.c.d"] = "one"
            config["a.b.c.e"] = "two"
            config["a.b"] = "three"
            context("when an exact prefix match is requested") {
                val props = config.getPropertyNamesByPrefix("a.b.c", exactPrefixMatch = true)
                should("retrieve the right properties") {
                    props shouldHaveSize 2
                    props shouldContainAll listOf("a.b.c.d", "a.b.c.e")
                }
            }
            context("when an exact prefix match is not requested") {
                val props = config.getPropertyNamesByPrefix("a", exactPrefixMatch = false)
                should("retrieve the right properties") {
                    props shouldHaveSize 3
                    props shouldContainAll listOf("a.b.c.d", "a.b.c.e", "a.b")
                }
            }
        }
    }
}

private class TestReadOnlyConfigurationService(
    override val properties: Properties = Properties()
) : AbstractReadOnlyConfigurationService(), MutableMap<Any, Any> by properties {
    override fun reloadConfiguration() {}
}
