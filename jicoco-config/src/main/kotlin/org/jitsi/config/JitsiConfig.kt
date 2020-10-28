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
import org.jitsi.metaconfig.ConfigSource
import org.jitsi.service.configuration.ConfigurationService
import org.jitsi.utils.logging2.LoggerImpl

/**
 * Holds common [ConfigSource] instances for retrieving configuration.
 *
 * Should be renamed to JitsiConfig once the old one is removed.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class JitsiConfig {
    companion object {
        val logger = LoggerImpl(JitsiConfig::class.simpleName)

        /**
         * A [ConfigSource] loaded via [ConfigFactory].
         */
        var TypesafeConfig: ConfigSource = TypesafeConfigSource("typesafe config", loadNewConfig())
            private set

        private var numTypesafeReloads = 0

        /**
         * The 'new' [ConfigSource] that should be used by configuration properties.  Able to be changed for testing.
         */
        private val _newConfig: ConfigSourceWrapper = ConfigSourceWrapper(TypesafeConfig).also {
            logger.info("Initialized newConfig: ${TypesafeConfig.description}")
        }
        val newConfig: ConfigSource
            get() = _newConfig

        /**
         * A [ConfigurationService] which can be installed via OSGi for legacy code which still requires it.
         */
        @JvmStatic
        val SipCommunicatorProps: ConfigurationService = ReadOnlyConfigurationService()

        /**
         * A [ConfigSource] wrapper around the legacy [ConfigurationService].
         */
        val SipCommunicatorPropsConfigSource: ConfigSource =
            ConfigurationServiceConfigSource("sip communicator props", SipCommunicatorProps)

        /**
         * The 'legacy' [ConfigSource] that should be used by configuration properties.  Able to be changed for testing.
         */
        private val _legacyConfig: ConfigSourceWrapper = ConfigSourceWrapper(SipCommunicatorPropsConfigSource).also {
            logger.info("Initialized legacyConfig: ${SipCommunicatorPropsConfigSource.description}")
        }
        val legacyConfig: ConfigSource
            get() = _legacyConfig

        fun useDebugNewConfig(config: ConfigSource) {
            logger.info("Replacing newConfig with ${config.description}")
            _newConfig.innerSource = config
        }

        fun useDebugLegacyConfig(config: ConfigSource) {
            logger.info("Replacing legacyConfig with ${config.description}")
            _legacyConfig.innerSource = config
        }

        private fun loadNewConfig(): Config {
            // Parse an application replacement (something passed via -Dconfig.file), if there is one
            return ConfigFactory.parseApplicationReplacement().orElse(ConfigFactory.empty())
                // Fallback to application.(conf|json|properties)
                .withFallback(ConfigFactory.parseResourcesAnySyntax("application"))
                // Fallback to reference.(conf|json|properties)
                .withFallback(ConfigFactory.defaultReference())
                .resolve()
        }

        fun reloadNewConfig() {
            logger.info("Reloading the Typesafe config source (previously reloaded $numTypesafeReloads times).")
            ConfigFactory.invalidateCaches()
            numTypesafeReloads++
            TypesafeConfig = TypesafeConfigSource(
                "typesafe config (reloaded $numTypesafeReloads times)",
                loadNewConfig()
            )
            _newConfig.innerSource = TypesafeConfig
        }
    }
}
