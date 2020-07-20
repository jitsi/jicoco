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

import org.jitsi.metaconfig.ConfigException
import org.jitsi.metaconfig.ConfigSource
import org.jitsi.service.configuration.ConfigurationService
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class ConfigurationServiceConfigSource(
    override val name: String,
    private val config: ConfigurationService
) : ConfigSource {

    /**
     * Note that we can't use getBoolean, getInt, etc. in [ConfigurationService] because they
     * all take a default value, which we don't want (because if the value isn't found we want
     * to throw [ConfigException.UnableToRetrieve.NotFound] so the calling code can fall back to
     * another property).
     */
    override fun getterFor(type: KType): (String) -> Any {
        return when(type) {
            typeOf<String>() -> { key -> config.getStringOrThrow(key) }
            typeOf<Boolean>() -> { key -> config.getStringOrThrow(key).toBoolean() }
            typeOf<Double>() -> { key -> config.getStringOrThrow(key).toDouble() }
            typeOf<Int>() -> { key -> config.getStringOrThrow(key).toInt() }
            typeOf<Long>() -> { key -> config.getStringOrThrow(key).toLong() }
            // We special handling Map<String, String> and interpret it as:
            // For the given prefix, return me all the properties which start
            // with that prefix mapped to their values (retrieved as Strings)
            typeOf<Map<String, String>>() -> { key ->
                val props = mutableMapOf<String, String>()
                for (propName in config.getPropertyNamesByPrefix(key, false)) {
                    props[propName] = config.getString(propName)
                }
                props
            }
            else -> throw ConfigException.UnsupportedType("Type $type not supported in source '$name'")
        }
    }

    private fun ConfigurationService.getStringOrThrow(key: String): String =
        getString(key) ?: throw ConfigException.UnableToRetrieve.NotFound("Key '$key' not found in source '$name'")
}
