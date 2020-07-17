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
import io.kotlintest.shouldBe
import io.kotlintest.specs.ShouldSpec
import kotlin.reflect.typeOf

class NewTypesafeConfigSourceTest : ShouldSpec() {

    init {
        "retrieving an enum" {
            val config = ConfigFactory.parseString("""
                color = BLUE
            """.trimIndent())
            val source = NewTypesafeConfigSource("test", config)
            should("work correctly") {
                source.getterFor(typeOf<Color>())("color") shouldBe Color.BLUE
            }
        }

    }
}

private enum class Color {
    RED,
    BLUE,
    GREEN
}
