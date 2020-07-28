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

import org.jitsi.service.configuration.ConfigurationService
import java.nio.file.Paths
import java.util.Properties

/**
 * An implementation of [AbstractReadOnlyConfigurationService] which supports reading
 * the properties from a file whose location is determined by the
 * [ConfigurationService.PNAME_SC_HOME_DIR_LOCATION] and [ConfigurationService.PNAME_SC_HOME_DIR_NAME]
 * properties.
 */
class ReadOnlyConfigurationService : AbstractReadOnlyConfigurationService() {
    override val properties: Properties = Properties()

    init {
        reloadConfiguration()
    }

    override fun reloadConfiguration() {
        val scHomeDirLocation = System.getenv(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION)
            ?: System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION)
            ?: run {
                logger.info("${ConfigurationService.PNAME_SC_HOME_DIR_LOCATION} not set")
                return
            }
        val scHomeDirName = System.getenv(ConfigurationService.PNAME_SC_HOME_DIR_NAME)
            ?: System.getProperty(ConfigurationService.PNAME_SC_HOME_DIR_NAME)
            ?: run {
                logger.info("${ConfigurationService.PNAME_SC_HOME_DIR_NAME} not set")
                return
            }
        val fileName = "sip-communicator.properties"
        with(Paths.get(scHomeDirLocation, scHomeDirName, fileName)) {
            logger.info("loading config file at path $this")
            try {
                val reader = toFile().bufferedReader()
                properties.load(reader)
            } catch (t: Throwable) {
                logger.info("Error loading config file: $t")
            }
        }
    }
}
