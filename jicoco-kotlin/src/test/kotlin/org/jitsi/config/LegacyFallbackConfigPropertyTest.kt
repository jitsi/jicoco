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

class LegacyFallbackConfigPropertyTest : JitsiConfigTest() {
    init {
        "Read once property" {
            runBasicTests(
                legacyConfigName = legacyName,
                legacyValueGenerator = IntMockConfigValueGenerator,
                newConfigName = newName,
                newConfigValueGenerator = IntMockConfigValueGenerator,
                propCreator = { TestReadOnceProperty() }
            )
        }
        "Read every time property" {
            runBasicTests(
                legacyConfigName = legacyName,
                legacyValueGenerator = IntMockConfigValueGenerator,
                legacyReadOnce = false,
                newConfigName = newName,
                newConfigValueGenerator = IntMockConfigValueGenerator,
                newReadOnce = false,
                propCreator = { TestReadEveryTimeProperty() }
            )
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
