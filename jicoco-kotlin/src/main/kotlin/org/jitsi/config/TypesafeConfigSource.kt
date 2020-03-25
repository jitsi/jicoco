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
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import org.jitsi.utils.config.ConfigSource
import org.jitsi.utils.config.exception.ConfigurationValueTypeUnsupportedException
import org.jitsi.utils.logging2.LoggerImpl
import java.time.Duration
import java.time.Period
import java.time.temporal.TemporalAmount
import kotlin.reflect.KClass

/**
 * A [ConfigSource] which reads from a typesafe [Config] object.
 */
open class TypesafeConfigSource(
    override val name: String,
    private val configLoader: () -> Config
) : ConfigSource {
    internal var config = configLoader()
        private set

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
        return when(valueType) {
            Boolean::class -> { path -> config.getBoolean(path) as T }
            Number::class -> { path -> config.getNumber(path) as T }
            Int::class -> { path -> config.getInt(path) as T }
            Long::class -> { path -> config.getLong(path) as T }
            Double::class -> { path -> config.getDouble(path) as T }
            String::class -> { path -> config.getString(path) as T }
            //TODO: enum...working on how to cast the type correctly
//            Enum::class -> { path ->
//                val c = valueType.java as Class<Enum>
//                config.getEnum(valueType.java as Class<Enum<T>>, path) as T
//            }
            // ConfigObject and ConfigValue are useful when parsing custom
            // or complex types, as we can first retrieve them as this type
            // and then transform them as needed
            ConfigObject::class -> { path -> config.getObject(path) as T }
            ConfigValue::class -> { path -> config.getValue(path) as T }
            Duration::class -> { path -> config.getDuration(path) as T }
            Period::class -> { path -> config.getPeriod(path) as T }
            TemporalAmount::class -> { path -> config.getTemporal(path) as T }
            //TODO: if we want to support the getXXXList methods, we'll need to do
            // some more work: we can't pass List<XXX> to this method since
            // we lose the inner generic type (List's generic type), so we won't
            // know which one to call.  Look into the method described here:
            // https://stackoverflow.com/a/37099526 (super class tokens).
            // In the meantime, users can retrieve as ConfigValue and do the
            // transformation manually
            else -> throw ConfigurationValueTypeUnsupportedException.new(valueType)
        }
    }
    //TODO: translate typesafeconfig exceptions(?)

    override fun reload() {
        config = configLoader()
    }

    override fun toStringMasked(): String = config.mask().root().render()
}

/**
 * The 'new' config file is read via the default [ConfigFactory.load]
 * loader.
 */
class NewConfig : TypesafeConfigSource("new config", {
    ConfigFactory.load().also {
        logger.info("Loaded NewConfig with origin: " + it.origin().description())
    }
}) {
    companion object {
        private val logger = LoggerImpl(NewConfig::class.java.name)
    }
}

/**
 * The 'legacy' config file is read by explicitly parsing the old file via
 * [LegacyConfigFileLoader.load]
 */
class LegacyConfig : TypesafeConfigSource("legacy config", ::loadLegacyConfig) {
    companion object {
        fun loadLegacyConfig(): Config {
            val oldConfigHomeDirLocation = System.getProperty("net.java.sip.communicator.SC_HOME_DIR_LOCATION")
            val oldConfigHomeDirName = System.getProperty("net.java.sip.communicator.SC_HOME_DIR_NAME")
            return LegacyConfigFileLoader.load(
                oldConfigHomeDirLocation,
                oldConfigHomeDirName,
                "sip-communicator.properties"
            )
        }
    }
}
