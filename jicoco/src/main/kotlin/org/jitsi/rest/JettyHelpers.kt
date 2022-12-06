@file:JvmName("JettyHelpers")
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

import jakarta.servlet.DispatcherType
import org.eclipse.jetty.http.HttpStatus
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.servlets.CrossOriginFilter
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.jitsi.utils.getJavaVersion
import java.nio.file.Paths
import java.util.EnumSet

/**
 * Create a non-secure Jetty server instance listening on the given [port] and [host] address.
 * [sendServerVersion] controls whether Jetty should send its server version in the error responses or not.
 */
fun createJettyServer(config: JettyBundleActivatorConfig): Server {
    val httpConfig = HttpConfiguration().apply {
        sendServerVersion = config.sendServerVersion
        addCustomizer { _, _, request ->
            if (request.method.equals("TRACE", ignoreCase = true)) {
                request.isHandled = true
                request.response.status = HttpStatus.METHOD_NOT_ALLOWED_405
            }
        }
    }
    val server = Server().apply {
        handler = ServletContextHandler()
    }
    val connector = ServerConnector(server, HttpConnectionFactory(httpConfig)).apply {
        port = config.port
        host = config.host
    }
    server.addConnector(connector)
    return server
}

/**
 * Create a secure Jetty server instance listening on the given [port] and [host] address and using the
 * KeyStore located at [keyStorePath], optionally protected by [keyStorePassword].  [needClientAuth] sets whether
 * client auth is needed for SSL (see [SslContextFactory.setNeedClientAuth]).
 * [sendServerVersion] controls whether Jetty should send its server version in the error responses or not.
 */
fun createSecureJettyServer(config: JettyBundleActivatorConfig): Server {
    val sslContextFactoryKeyStoreFile = Paths.get(config.keyStorePath!!).toFile()
    val sslContextFactory = SslContextFactory.Server().apply {
        val tlsProtocols = if (supportsTls13()) {
            config.tlsProtocols
        } else {
            config.tlsProtocols.filterNot { it == JettyBundleActivatorConfig.TLS_1_3 }
        }
        setIncludeProtocols(*tlsProtocols.toTypedArray())
        setIncludeCipherSuites(*config.tlsCipherSuites.toTypedArray())

        isRenegotiationAllowed = false
        if (config.keyStorePassword != null) {
            keyStorePassword = config.keyStorePassword
        }
        keyStorePath = sslContextFactoryKeyStoreFile.path
        needClientAuth = config.needClientAuth
    }
    val httpConfig = HttpConfiguration().apply {
        securePort = config.tlsPort
        secureScheme = "https"
        addCustomizer(SecureRequestCustomizer())
        sendServerVersion = config.sendServerVersion
    }
    val server = Server().apply {
        handler = ServletContextHandler()
    }

    val connector = ServerConnector(
        server,
        SslConnectionFactory(
            sslContextFactory,
            "http/1.1"
        ),
        HttpConnectionFactory(httpConfig)
    ).apply {
        host = config.host
        port = config.tlsPort
    }
    server.addConnector(connector)
    return server
}

/**
 * Create a Jetty [Server] instance based on the given [config].
 */
fun createServer(config: JettyBundleActivatorConfig): Server {
    return if (config.isTls) {
        createSecureJettyServer(config)
    } else {
        createJettyServer(config)
    }
}

fun JettyBundleActivatorConfig.isEnabled(): Boolean = port != -1 || tlsPort != -1

// Note: it's technically possible that this cast fails, but
// shouldn't happen in practice given that the above methods always install
// a ServletContextHandler handler.
val Server.servletContextHandler: ServletContextHandler
    get() = handler as ServletContextHandler

fun ServletContextHandler.enableCors(pathSpec: String = "/*") {
    addFilter(CrossOriginFilter::class.java, pathSpec, EnumSet.of(DispatcherType.REQUEST)).apply {
        setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*")
        setInitParameter(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, "*")
        setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST")
        setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "X-Requested-With,Content-Type,Accept,Origin")
    }
}

fun Server.addServlet(servlet: ServletHolder, pathSpec: String) =
    this.servletContextHandler.addServlet(servlet, pathSpec)

// TLS 1.3 requires Java 11 or later.
private fun supportsTls13(): Boolean {
    return try {
        getJavaVersion() >= 11
    } catch (t: Throwable) {
        false
    }
}
