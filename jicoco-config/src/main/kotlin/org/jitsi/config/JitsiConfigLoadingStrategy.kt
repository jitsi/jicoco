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
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigLoadingStrategy
import com.typesafe.config.ConfigParseOptions
import java.io.File
import java.net.MalformedURLException
import java.net.URL

class JitsiConfigLoadingStrategy  : ConfigLoadingStrategy {
    override fun parseApplicationConfig(parseOptions: ConfigParseOptions): Config {
        println("====> IN CUSTOM LOADER")
        val loader = parseOptions.classLoader ?: throw ConfigException.BugOrBroken(
            "ClassLoader should have been set here; bug in ConfigFactory. " +
                    "(You can probably work around this bug by passing in a class loader or calling " +
                    "currentThread().setContextClassLoader() though.)")

        // Load both any file specified by config.file, config.resource or config.url and any application
        // resource
        var specified = 0
        val resource = System.getProperty("config.resource").also {
            if (it != null) specified++
        }
        val file = System.getProperty("config.file").also {
            if (it != null) specified++
        }
        val url = System.getProperty("config.url").also {
            if (it != null) specified++
        }
        if (specified == 0) {
            return ConfigFactory.parseResourcesAnySyntax("application")
        } else {
            // the override file/url/resource MUST be present or it's an error
            val overrideOptions = parseOptions.setAllowMissing(false)
            return when {
                resource != null -> {
                    ConfigFactory.parseResources(loader, resource.removePrefix("/"), overrideOptions)
                        .withFallback(ConfigFactory.parseResourcesAnySyntax("application"))
                        .resolve()
                }
                file != null -> {
                    ConfigFactory.parseFile(File(file), overrideOptions)
                        .withFallback(ConfigFactory.parseResourcesAnySyntax("application"))
                        .resolve()
                }
                else -> {
                    try {
                        ConfigFactory.parseURL(URL(url), overrideOptions)
                            .withFallback(ConfigFactory.parseResourcesAnySyntax("application"))
                            .resolve()
                    } catch (e: MalformedURLException) {
                        throw ConfigException.Generic("Bad URL in config.url system property: '"
                                + url + "': " + e.message, e)
                    }
                }
            }
        }
    }
}
