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

import org.jitsi.utils.config.ConfigProperty
import org.jitsi.utils.config.ConfigSource
import org.jitsi.utils.config.dsl.ConfigPropertyBuilder
import org.jitsi.utils.config.dsl.MultiConfigPropertyBuilder
import kotlin.reflect.KClass

/**
 * Models a property set in a legacy config file under one name and a new
 * config file under another name which doesn't need to transform the value
 * in any way
 */
open class SimpleConfig<T : Any>(
    valueType: KClass<T>,
    legacyName: String,
    newName: String,
    readOnce: Boolean,
    // Allow these to be passed in for testing
    legacyConfig: ConfigSource = JitsiConfig.legacyConfig,
    newConfig: ConfigSource = JitsiConfig.newConfig
) : ConfigProperty<T> {
    private val multiProp = MultiConfigPropertyBuilder(valueType).apply {
        property {
            name(legacyName)
            if (readOnce) readOnce() else readEveryTime()
            fromConfig(legacyConfig)
        }
        property {
            name(newName)
            if (readOnce) readOnce() else readEveryTime()
            fromConfig(newConfig)
        }
    }.build()

    override val value: T
        get() = multiProp.value
}

// A helper to create an instance of SimpleConfig
inline fun <reified T : Any> simple(readOnce: Boolean, legacyName: String, newName: String): ConfigProperty<T> =
    SimpleConfig(T::class, legacyName, newName, readOnce)


fun <T : Any> MultiConfigPropertyBuilder<T>.legacyProperty(block: ConfigPropertyBuilder<T>.() -> Unit) {
    property {
        fromConfig(JitsiConfig.legacyConfig)
        block()
    }
}

fun <T : Any> MultiConfigPropertyBuilder<T>.newProperty(block: ConfigPropertyBuilder<T>.() -> Unit) {
    property {
        fromConfig(JitsiConfig.newConfig)
        block()
    }
}
