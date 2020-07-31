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
import java.io.StringReader
import java.util.Properties

/**
 * Execute the given [block] using the props defined by [props] as a legacy
 * [org.jitsi.metaconfig.ConfigSource] with name "legacy".  Resets the legacy
 * config to empty after [block] is executed.
 */
inline fun useLegacyConfig(props: String, block: () -> Unit) =
    useLegacyConfig("legacy", props, block)

/**
 * Execute the given [block] using the props defined by [props] as a legacy
 * [org.jitsi.metaconfig.ConfigSource] with name [name].  Resets the legacy
 * config to empty after [block] is executed.
 */
inline fun useLegacyConfig(name: String, props: String, block: () -> Unit) {
    setLegacyConfig(props = props, name = name)
    block()
    setLegacyConfig("")
}

/**
 * Execute the given [block] using the config defined by [config] as a new
 * [org.jitsi.metaconfig.ConfigSource], falling back to the defaults if
 * [loadDefaults] is true, with name "new".  Resets the new config to empty
 * after [block] is executed.
 */
inline fun useNewConfig(config: String, loadDefaults: Boolean, block: () -> Unit) =
    useNewConfig("new", config, loadDefaults, block)

/**
 * Execute the given [block] using the config defined by [config] as a new
 * [org.jitsi.metaconfig.ConfigSource], falling back to the defaults if
 * [loadDefaults] is true, with name [name].  Resets the new config to empty
 * after [block] is executed.
 */
inline fun useNewConfig(name: String, config: String, loadDefaults: Boolean, block: () -> Unit) {
    setNewConfig(config, loadDefaults, name)
    block()
    setNewConfig("", false)
}

inline fun useNewConfig(config: String, block: () -> Unit) =
    useNewConfig(config, false, block)


/**
 * Creates a [TypesafeConfigSource] using the parsed value of [config] and
 * defaults in reference.conf if [loadDefaults] is set with name [name] and
 * sets it as the underlying source of [JitsiConfig.newConfig]
 */
fun setNewConfig(config: String, loadDefaults: Boolean, name: String = "new") {
    JitsiConfig.useDebugNewConfig(
        TypesafeConfigSource(
            name,
            ConfigFactory.parseString(config).run { if (loadDefaults) withFallback(ConfigFactory.load()) else this }
        )
    )
}

/**
 * Creates a [ReadOnlyConfigurationService] using the parsed value of [props]
 * with name [name] and sets it as the underlying source of [JitsiConfig.legacyConfig]
 */
fun setLegacyConfig(props: String, name: String = "legacy") {
    JitsiConfig.useDebugLegacyConfig(
        ConfigurationServiceConfigSource(
            name,
            TestReadOnlyConfigurationService(Properties().apply { load(StringReader(props)) })
        )
    )
}
