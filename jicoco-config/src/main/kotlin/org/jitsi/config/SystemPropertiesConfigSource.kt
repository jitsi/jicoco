/*
 * Copyright @ 2020 - present 8x8, Inc.
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
import java.lang.NumberFormatException
import kotlin.reflect.typeOf

class SystemPropertiesConfigSource() : ConfigSource {
    override val name = "System Properties ConfigSource"

    override fun getterFor(type: kotlin.reflect.KType): (String) -> Any {

        return when (type) {
            // Maintain compatibility with ice4j (which uses Boolean.parseBoolean)
            typeOf<Boolean>() -> { key -> java.lang.Boolean.parseBoolean(getString(key)) }
            typeOf<Int>() -> wrapNfe { key -> Integer.parseInt(getString(key)) }
            typeOf<Long>() -> wrapNfe { key -> java.lang.Long.parseLong(getString(key)) }
            typeOf<Double>() -> wrapNfe { key -> java.lang.Double.parseDouble(getString(key)) }
            typeOf<String>() -> { key -> getString(key) }
            else -> throw ConfigException.UnsupportedType("Type $type unsupported")
        }
    }

    private fun getString(key: String): String
        = System.getProperty(key) ?:
            throw ConfigException.UnableToRetrieve.NotFound("No system property with name '$key' is set.")

    private fun wrapNfe(block: (String) -> Any): (String) -> Any {
        return { key ->
            try {
                block(key)
            } catch (e: NumberFormatException) {
                throw ConfigException.UnableToRetrieve.WrongType("Can not parse the value of '$key' as a number.")
            } catch (e: ConfigException) {
                throw e
            } catch (t: Throwable) {
                throw ConfigException.UnableToRetrieve.Error(t)
            }
        }
    }
}