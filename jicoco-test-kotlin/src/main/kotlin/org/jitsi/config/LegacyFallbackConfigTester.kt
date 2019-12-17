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
import org.jitsi.utils.config.ConfigProperty

fun <LegacyConfigType : Any, NewConfigType : Any, PropType : Any> ConfigTest.runBasicTests(
    legacyConfigName: String,
    legacyValueGenerator: MockConfigValueGenerator<LegacyConfigType, PropType>,
    legacyReadOnce: Boolean = true,
    newConfigName: String,
    newConfigValueGenerator: MockConfigValueGenerator<NewConfigType, PropType>,
    newReadOnce: Boolean = true,
    propCreator: () -> ConfigProperty<PropType>
) {
    val originalLegacyConfigValue = legacyValueGenerator.gen()
    val legacyConfigWithValue = MockConfigSource("legacy",
        mapOf(legacyConfigName to originalLegacyConfigValue.configValue)
    )

    val originalNewConfigValue = newConfigValueGenerator.gen()
    val newConfig = MockConfigSource("new",
        mapOf(newConfigName to originalNewConfigValue.configValue)
    )

    run {
        // Old config has value, new config has value
        withLegacyConfig(legacyConfigWithValue)
        withNewConfig(newConfig)
        val prop = propCreator()
        withClue("When both old config and new config have a value") {
            prop.value shouldBe originalLegacyConfigValue.propValue
        }
        // Change the value in the legacy config
        val newLegacyConfigValue = legacyValueGenerator.gen()
        legacyConfigWithValue[legacyConfigName] = newLegacyConfigValue.configValue
        if (legacyReadOnce) {
            withClue("Legacy field is marked as read-once, changes in legacy config shouldn't be reflected") {
                prop.value shouldBe originalLegacyConfigValue.propValue
            }
        } else {
            withClue("Legacy field is marked as read-every-time, changes in legacy config should be reflected") {
                prop.value shouldBe newLegacyConfigValue.propValue
            }
        }
    }

    run {
        // Old config has no value, new config has value
        withLegacyConfig(EMPTY_CONFIG)
        withNewConfig(newConfig)
        val prop = propCreator()
        withClue("When only new config has a value") {
            prop.value shouldBe originalNewConfigValue.propValue
        }
        // Change the value in the legacy config
        val newNewConfigValue = newConfigValueGenerator.gen()
        newConfig[newConfigName] = newNewConfigValue.configValue
        if (newReadOnce) {
            withClue("New field is marked as read-once, changes in new config shouldn't be reflected") {
                prop.value shouldBe originalNewConfigValue.propValue
            }
        } else {
            withClue("New field is marked as read-every-time, changes in new config should be reflected") {
                prop.value shouldBe newNewConfigValue.propValue
            }
        }
    }
}

