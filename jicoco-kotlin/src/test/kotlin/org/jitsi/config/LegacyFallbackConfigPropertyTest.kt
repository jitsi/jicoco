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

import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import org.jitsi.videobridge.testutils.EMPTY_CONFIG
import org.jitsi.videobridge.testutils.MapConfigSource

class LegacyFallbackConfigPropertyTest : ConfigTest() {
    private val legacyConfigNoValue =
        MapConfigSource("legacy config", mapOf("some.other.prop.name" to 42))
    private val legacyConfigWithValue =
        MapConfigSource("legacy config", mapOf(legacyName to legacyValue))
    private val newConfig =
        MapConfigSource("new config", mapOf(newName to newValue))

    init {
        "when old config is present" {
            "but doesn't provide a value" {
                withLegacyConfig(legacyConfigNoValue)
                "and the new config does" {
                    withNewConfig(newConfig)
                    should("get the value from new config") {
                        testProps(newName, newConfig)
                    }
                }
            }
            "and provides a value" {
                withLegacyConfig(legacyConfigWithValue)
                "and so does the new one" {
                    withNewConfig(newConfig)
                    should("get the value from old config") {
                        testProps(legacyName, legacyConfigWithValue)
                    }
                }
            }
        }
        "when there's no old config" {
            withLegacyConfig(EMPTY_CONFIG)
            "and new config provides a value" {
                withNewConfig(newConfig)
                testProps(newName, newConfig)
            }
        }
    }

    private fun testProps(
        expectedKey: String,
        expectedSourceOfValue: MapConfigSource
    ) {
        println("Legacy config:\n${JitsiConfig.legacyConfig.toStringMasked()}")
        println("new config wrapper instance: ${JitsiConfig.newConfig.hashCode()}")
        println("New config:\n${JitsiConfig.newConfig.toStringMasked()}")

        val readOnceProp = TestReadOnceProperty()
        val readEveryTimeProp = TestReadEveryTimeProperty()
        val originalExpectedValue = expectedSourceOfValue.getterFor(Int::class).invoke(expectedKey)

        for (prop in listOf(readOnceProp, readEveryTimeProp)) {
            withClue("${prop.javaClass.simpleName} should read the correct value") {
                prop.value shouldBe originalExpectedValue
            }
        }
        // 4242 is assumed to be some value different than whatever the value was before
        println("Modifying config instance ${expectedSourceOfValue.hashCode()}")
        expectedSourceOfValue[expectedKey] = 4242
        withClue("ReadOnceProp should always see the original value") {
            readOnceProp.value shouldBe originalExpectedValue
        }
        withClue("ReadEveryTimeProp should always see the new value") {
            readEveryTimeProp.value shouldBe 4242
        }
    }

    companion object {
        private const val legacyName = "some.legacy.prop.name"
        private const val legacyValue = 5
        private const val newName = "some.new.prop.name"
        private const val newValue = 10
    }

    private class TestReadOnceProperty : LegacyFallbackConfigProperty<Int>(
        valueType = Int::class,
        legacyName = legacyName,
        newName = newName,
        readOnce = true
    )

    private class TestReadEveryTimeProperty : LegacyFallbackConfigProperty<Int>(
        valueType = Int::class,
        legacyName = legacyName,
        newName = newName,
        readOnce = false
    )
}
