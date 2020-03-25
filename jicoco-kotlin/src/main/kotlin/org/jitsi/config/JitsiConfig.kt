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
import org.jitsi.utils.config.ConfigSource
import org.jitsi.utils.logging2.LoggerImpl

/**
 * Creates and holds the [ConfigSource] instances for the legacy and new
 * config files.
 */
class JitsiConfig {
    companion object {
        private val logger = LoggerImpl(JitsiConfig::class.qualifiedName)
        val newConfig: ConfigSource = JitsiConfigFactory.newConfigSupplier()
        val legacyConfig: ConfigSource = JitsiConfigFactory.legacyConfigSupplier()
        @JvmStatic
        val legacyConfigShim = JitsiConfigFactory.legacyConfigurationServiceShimSupplier()

        init {
            dumpConfigs()
        }

        fun reload() {
            logger.info("Reloading.")
            ConfigFactory.invalidateCaches()
            newConfig.reload()
            legacyConfig.reload()
            legacyConfigShim.reloadConfiguration()
            dumpConfigs()
        }

        private fun dumpConfigs() {
            logger.debug {"Loaded legacy config:\n${legacyConfig.toStringMasked()}"}
            logger.debug {"Loaded legacy shim config:\n${legacyConfigShim.toStringMasked()}" }
            logger.debug {"Loaded new config:\n${newConfig.toStringMasked()}" }
        }
    }
}
