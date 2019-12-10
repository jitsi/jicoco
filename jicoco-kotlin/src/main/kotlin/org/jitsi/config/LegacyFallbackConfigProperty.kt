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

import org.jitsi.utils.config.ConfigPropertyAttributes
import org.jitsi.utils.config.ConfigPropertyAttributesBuilder
import org.jitsi.utils.config.FallbackProperty
import org.jitsi.utils.config.helpers.attributes
import kotlin.reflect.KClass

/**
 * Models a property set in a legacy config file under one name and a new
 * config file under another name which doesn't need to transform the value
 * in any way
 */
open class LegacyFallbackConfigProperty<T : Any>(
    valueType: KClass<T>,
    legacyName: String,
    newName: String,
    readOnce: Boolean
) : FallbackProperty<T>(
    legacyConfigAttributes(valueType) {
        name(legacyName)
        if (readOnce) readOnce() else readEveryTime()
    },
    newConfigAttributes(valueType) {
        name(newName)
        if (readOnce) readOnce() else readEveryTime()
    }
)

inline fun <reified T : Any> legacyConfigAttributes(
    noinline block: ConfigPropertyAttributesBuilder<T>.() -> Unit
): ConfigPropertyAttributes<T> = legacyConfigAttributes(T::class, block)

fun <T : Any> legacyConfigAttributes(
    valueType: KClass<T>,
    block: ConfigPropertyAttributesBuilder<T>.() -> Unit
): ConfigPropertyAttributes<T> {
    return with (ConfigPropertyAttributesBuilder(valueType)) {
        block()
        fromConfig(JitsiConfig.legacyConfig)
        build()
    }
}

inline fun <reified T : Any> newConfigAttributes(
    noinline block: ConfigPropertyAttributesBuilder<T>.() -> Unit
): ConfigPropertyAttributes<T> = newConfigAttributes(T::class, block)

fun <T : Any> newConfigAttributes(
    valueType: KClass<T>,
    block: ConfigPropertyAttributesBuilder<T>.() -> Unit
): ConfigPropertyAttributes<T> {
    return with (ConfigPropertyAttributesBuilder(valueType)) {
        block()
        fromConfig(JitsiConfig.newConfig)
        build()
    }
}

