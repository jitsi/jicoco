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

import io.kotlintest.specs.ShouldSpec
import org.jitsi.service.configuration.ConfigurationService

class AbstractReadOnlyConfigurationServiceTest : ShouldSpec() {

    init {
        System.setProperty(ConfigurationService.PNAME_SC_HOME_DIR_LOCATION, "/Users/bbaldino")
        System.setProperty(ConfigurationService.PNAME_SC_HOME_DIR_NAME, ".sip-communicator")
        val config = ReadOnlyConfigurationService()
        println(config.allPropertyNames)

        println(config.getString("hello.world"))
        println(config.getPropertyNamesByPrefix("org.jitsi.videobridge", true))
    }
}
