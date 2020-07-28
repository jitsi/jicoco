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
import org.jitsi.service.configuration.ConfigurationService

/**
 * Holds common [ConfigSource] instances for retrieving configuration.
 *
 * Should be renamed to JitsiConfig once the old one is removed.
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
class JitsiConfig {
    companion object {
        /**
         * A [ConfigSource] loaded via [ConfigFactory].
         */
        val TypesafeConfig: ConfigSource = TypesafeConfigSource("typesafe config", ConfigFactory.load())

        /**
         * The 'new' [ConfigSource] that should be used by configuration properties.  Able to be changed for testing.
         */
        var newConfig = TypesafeConfig

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
        var legacyConfig = SipCommunicatorPropsConfigSource
    }
}
