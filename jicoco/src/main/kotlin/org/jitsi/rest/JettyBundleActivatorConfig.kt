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

package org.jitsi.rest

import org.jitsi.config.JitsiConfig
import org.jitsi.metaconfig.config
import org.jitsi.metaconfig.optionalconfig

/**
 * Configuration properties used by [AbstractJettyBundleActivator]
 */
class JettyBundleActivatorConfig(
    private val legacyPropertyPrefix: String,
    private val newPropertyPrefix: String
) {
    /**
     * The port on which the Jetty server is to listen for HTTP requests
     */
    val port: Int by config {
        "$legacyPropertyPrefix.jetty.port".from(JitsiConfig.legacyConfig)
        "$newPropertyPrefix.port".from(JitsiConfig.newConfig)
        "default" { 8080 }
    }

    /**
     * The address on which the Jetty server will listen
     */
    val host: String? by optionalconfig {
        "$legacyPropertyPrefix.jetty.host".from(JitsiConfig.legacyConfig)
        "$newPropertyPrefix.host".from(JitsiConfig.newConfig)
    }

    /**
     * The [java.security.KeyStore] path to be utilized by [org.eclipse.jetty.util.ssl.SslContextFactory]
     * when Jetty serves over HTTPS.
     */
    val keyStorePath: String? by optionalconfig {
        "$legacyPropertyPrefix.jetty.sslContextFactory.keyStorePath".from(JitsiConfig.legacyConfig)
        "$newPropertyPrefix.key-store-path".from(JitsiConfig.newConfig)
    }

    /**
     * Whether or not this server should use TLS
     */
    val isTls: Boolean
        get() = keyStorePath != null

    /**
     * The [java.security.KeyStore] password to be used by [org.eclipse.jetty.util.ssl.SslContextFactory]
     * when Jetty serves over HTTPS
     */
    val keyStorePassword: String? by optionalconfig {
        "$legacyPropertyPrefix.jetty.sslContextFactory.keyStorePassword".from(JitsiConfig.legacyConfig)
        "$newPropertyPrefix.key-store-password".from(JitsiConfig.newConfig)
    }

    /**
     * Whether or not client certificate authentication is to be required by
     * [org.eclipse.jetty.util.ssl.SslContextFactory] when Jetty serves over HTTPS
     */
    val needClientAuth: Boolean by config {
        "$legacyPropertyPrefix.jetty.sslContextFactory.needClientAuth".from(JitsiConfig.legacyConfig)
        "$newPropertyPrefix.need-client-auth".from(JitsiConfig.newConfig)
        "default" { false }
    }

    /**
     * The port on which the Jetty server is to listen for HTTPS requests
     */
    val tlsPort: Int by config {
        "$legacyPropertyPrefix.jetty.tls.port".from(JitsiConfig.legacyConfig)
        "$newPropertyPrefix.tls-port".from(JitsiConfig.newConfig)
        "default" { 8443 }
    }
}
