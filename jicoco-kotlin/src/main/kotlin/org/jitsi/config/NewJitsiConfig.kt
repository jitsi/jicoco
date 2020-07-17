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
import org.jitsi.metaconfig.ConfigSource

class NewJitsiConfig {
    companion object {
        val TypesafeConfig: ConfigSource = NewTypesafeConfigSource("typesafe config", ConfigFactory.load())

        var newConfig = TypesafeConfig

        private val oldConfigHomeDirLocation = System.getProperty("net.java.sip.communicator.SC_HOME_DIR_LOCATION")
        private val oldConfigHomeDirName = System.getProperty("net.java.sip.communicator.SC_HOME_DIR_NAME")
        val SipCommunicatorProps: ConfigSource = NewTypesafeConfigSource(
            "legacy config",
            LegacyConfigFileLoader.load(
                oldConfigHomeDirLocation,
                oldConfigHomeDirName,
                "sip-communicator.properties"
            )
        )

        var legacyConfig = SipCommunicatorProps
    }
}
