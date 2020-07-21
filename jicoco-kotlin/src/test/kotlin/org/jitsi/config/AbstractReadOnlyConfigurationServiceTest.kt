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

import io.kotlintest.IsolationMode
import io.kotlintest.extensions.system.withSystemProperties
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import java.util.Properties

class AbstractReadOnlyConfigurationServiceTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val config = TestReadOnlyConfigurationService()

    init {
        "retrieving a property" {
            "present in both the config and the system" {
                config["some.prop"] = "42"
                withSystemProperties("some.prop" to "43") {
                    should("use the system property") {
                        config.getInt("some.prop", 0) shouldBe 42
                    }
                }
            }
            "not present anywhere" {
                should("return null") {
                    config.getProperty("missing") shouldBe null
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