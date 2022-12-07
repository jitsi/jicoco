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

    /**
     * Whether Jetty server version should be sent in HTTP responses
     */
    val sendServerVersion: Boolean by config {
        "$newPropertyPrefix.send-server-version".from(JitsiConfig.newConfig)
        "default" { true }
    }

    val tlsProtocols: List<String> by config {
        "$newPropertyPrefix.tls-protocols".from(JitsiConfig.newConfig)
        "default" { DEFAULT_TLS_PROTOCOLS }
    }

    val tlsCipherSuites: List<String> by config {
        "$newPropertyPrefix.tls-cipher-suites".from(JitsiConfig.newConfig)
        "default" { DEFAULT_TLS_CIPHER_SUITES }
    }

    override fun toString() = "host=$host, port=$port, tlsPort=$tlsPort, isTls=$isTls, keyStorePath=$keyStorePath, " +
        "sendServerVersion=$sendServerVersion, $tlsProtocols=$tlsProtocols, tlsCipherSuites=$tlsCipherSuites"

    companion object {
        val TLS_1_2 = "TLSv1.2"
        val TLS_1_3 = "TLSv1.3"
        val DEFAULT_TLS_PROTOCOLS = listOf(TLS_1_2, TLS_1_3)
        val DEFAULT_TLS_CIPHER_SUITES = listOf(
            "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
            "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",
            "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256"
        )
    }
}
