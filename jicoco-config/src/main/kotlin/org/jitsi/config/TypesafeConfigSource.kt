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
import com.typesafe.config.ConfigObject
import org.jitsi.metaconfig.ConfigException
import org.jitsi.metaconfig.ConfigSource
import java.time.Duration
import java.util.regex.Pattern
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

/**
 * A [ConfigSource] implementation backed by a [Config] instance.
 */
class TypesafeConfigSource(
    override val name: String,
    private val config: Config
) : ConfigSource {

    override val description: String = config.origin().description()

    override fun getterFor(type: KType): (String) -> Any {
        if (type.isSubtypeOf(typeOf<Enum<*>>())) {
            @Suppress("UNCHECKED_CAST")
            return getterForEnum(type.classifier as KClass<Nothing>)
        }
        return when (type) {
            typeOf<Boolean>() -> wrap { key ->
                // Typesafe is case-sensitive and does not accept "True" or "False" as valid boolean values.
                when (config.getString(key).lowercase()) {
                    "true" -> true
                    "false" -> false
                    else -> config.getBoolean(key)
                }
            }
            typeOf<Int>() -> wrap { key -> config.getInt(key) }
            typeOf<Long>() -> wrap { key -> config.getLong(key) }
            // Support expressions such as "5%"
            typeOf<Double>() -> wrap { key ->
                try {
                    config.getDouble(key)
                } catch (wrongTypeException: com.typesafe.config.ConfigException.WrongType) {
                    val stringValue: String = config.getString(key).trim()
                    if (stringValue.endsWith("%")) {
                        try {
                            0.01 * stringValue.dropLast(1).toDouble()
                        } catch (e: Throwable) {
                            throw wrongTypeException
                        }
                    } else {
                        throw wrongTypeException
                    }
                }
            }
            typeOf<String>() -> wrap { key -> config.getString(key) }
            typeOf<List<String>>() -> wrap { key -> config.getStringList(key) }
            typeOf<List<Int>>() -> wrap { key -> config.getIntList(key) }
            typeOf<Duration>() -> wrap { key -> config.getDuration(key) }
            typeOf<ConfigObject>() -> wrap { key -> config.getObject(key) }
            typeOf<List<Config>>() -> wrap { key -> config.getConfigList(key) }
            typeOf<Pattern>() -> wrap { key -> Pattern.compile(config.getString(key)) }
            else -> throw ConfigException.UnsupportedType("Type $type unsupported")
        }
    }

    private fun <T : Enum<T>> getterForEnum(clazz: KClass<T>): (String) -> Any {
        return wrap { key -> config.getEnum(clazz.java, key) }
    }

    /**
     * Translate [com.typesafe.config.ConfigException]s into [ConfigException]
     */
    private fun wrap(block: (String) -> Any): (String) -> Any {
        return { key ->
            try {
                block(key)
            } catch (e: com.typesafe.config.ConfigException.Missing) {
                throw ConfigException.UnableToRetrieve.NotFound("Key '$key' not found in source '$name'")
            } catch (e: com.typesafe.config.ConfigException.WrongType) {
                throw ConfigException.UnableToRetrieve.WrongType("Key '$key' in source '$name': ${e.message}")
            } catch (e: com.typesafe.config.ConfigException) {
                throw ConfigException.UnableToRetrieve.NotFound(e.message ?: "typesafe exception: ${e::class}")
            } catch (t: Throwable) {
                throw ConfigException.UnableToRetrieve.Error(t)
            }
        }
    }
}
