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
import com.typesafe.config.ConfigValueFactory
import io.kotlintest.IsolationMode
import io.kotlintest.specs.ShouldSpec
import io.kotlintest.shouldBe
import org.jitsi.utils.ConfigUtils

class ConfigExtensionsKtTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    init {
        "mask" {
            val config = ConfigFactory.parseString("""
                a {
                    pass-prop = s3cr3t
                    normal-prop = 10
                    b {
                        nested-pass-prop = 42
                        nested-normal-prop = hello
                    }
                }
            """.trimIndent())
            "with a set field regex" {
                ConfigUtils.PASSWORD_SYS_PROPS = "pass"
                val maskedConfig = config.mask()
                should("mask out the right values") {
                    maskedConfig.getString("a.pass-prop") shouldBe MASK
                    maskedConfig.getString("a.b.nested-pass-prop") shouldBe MASK
                }
                should("not mask out other values") {
                    maskedConfig.getInt("a.normal-prop") shouldBe 10
                    maskedConfig.getString("a.b.nested-normal-prop") shouldBe "hello"
                }
                should("not affect the original config") {
                    config.getString("a.pass-prop") shouldBe "s3cr3t"
                    config.getInt("a.normal-prop") shouldBe 10
                    config.getInt("a.b.nested-pass-prop") shouldBe 42
                    config.getString("a.b.nested-normal-prop") shouldBe "hello"
                }
            }
            "when the field regex is null" {
                ConfigUtils.PASSWORD_SYS_PROPS = null
                val maskedConfig = config.mask()
                "should not change anything" {
                    maskedConfig.getString("a.pass-prop") shouldBe "s3cr3t"
                    maskedConfig.getInt("a.normal-prop") shouldBe 10
                    maskedConfig.getInt("a.b.nested-pass-prop") shouldBe 42
                    maskedConfig.getString("a.b.nested-normal-prop") shouldBe "hello"
                }
            }
        }
        "with only origin" {
            val config = run {
                ConfigFactory.parseString("").
                    withValue("a.prop1", ConfigValueFactory.fromAnyRef(1, "origin-1")).
                    withValue("a.prop2", ConfigValueFactory.fromAnyRef(1, "origin-2")).
                    withValue("a.b.prop3", ConfigValueFactory.fromAnyRef(1, "origin-1")).
                    withValue("a.b.prop4", ConfigValueFactory.fromAnyRef(1, "origin-2"))
            }

            val origin1Config = config.withOnlyOrigin("origin-1")
            should("only keep properties from the requested origin") {
                origin1Config.entrySet().size shouldBe 2
                origin1Config.getInt("a.prop1") shouldBe 1
                origin1Config.getInt("a.b.prop3") shouldBe 1
            }
            should("not affect the original config instance") {
                config.entrySet().size  shouldBe 4
                config.getInt("a.prop1") shouldBe 1
                config.getInt("a.prop2") shouldBe 1
                config.getInt("a.b.prop3") shouldBe 1
                config.getInt("a.b.prop4") shouldBe 1
            }
        }
    }
}
