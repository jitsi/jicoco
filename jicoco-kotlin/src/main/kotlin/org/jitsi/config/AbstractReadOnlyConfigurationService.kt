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

import org.jitsi.service.configuration.ConfigVetoableChangeListener
import org.jitsi.service.configuration.ConfigurationService
import java.beans.PropertyChangeListener
import java.util.Properties

/**
 * A stripped-down implementation of [ConfigurationService] which serves two purposes:
 * 1) Injected as an OSGi implementation of [ConfigurationService] for libs which still
 * expect to find a [ConfigurationService] via OSGi
 * 2) Wrapped by [ConfigurationServiceConfigSource] to be used in new config
 *
 * NOTE: this abstract base class exists so that a test implementation can be written
 * which pulls [properties] from somewhere else.
 */
abstract class AbstractReadOnlyConfigurationService : ConfigurationService {
    protected abstract val properties: Properties

    override fun getString(propertyName: String): String? =
        getProperty(propertyName)?.toString()?.trim()

    override fun getString(propertyName: String, defaultValue: String?): String? =
        getString(propertyName) ?: defaultValue

    override fun getBoolean(propertyName: String, defaultValue: Boolean): Boolean =
        getString(propertyName)?.toBoolean() ?: defaultValue

    override fun getDouble(propertyName: String, defaultValue: Double): Double =
        getString(propertyName)?.toDouble() ?: defaultValue

    override fun getInt(propertyName: String, defaultValue: Int): Int =
        getString(propertyName)?.toInt() ?: defaultValue

    override fun getLong(propertyName: String, defaultValue: Long): Long =
        getString(propertyName)?.toLong() ?: defaultValue

    override fun getAllPropertyNames(): MutableList<String> =
        properties.keys.map { it as String}.toMutableList()

    override fun getProperty(propertyName: String): Any? {
        return properties[propertyName] ?: System.getProperty(propertyName)
    }

    override fun getPropertyNamesByPrefix(prefix: String, exactPrefixMatch: Boolean): MutableList<String> {
        val matchingPropNames = mutableListOf<String>()
        for (name in allPropertyNames) {
            if (exactPrefixMatch) {
                if (name.substringBeforeLast(delimiter = ".", missingDelimiterValue = "") == prefix) {
                    matchingPropNames += name
                }
            } else if (name.startsWith(prefix)) {
                matchingPropNames += name
            }
        }
        return matchingPropNames
    }

    override fun logConfigurationProperties(passwordPattern: String) {
        val regex = Regex(passwordPattern).takeIf { passwordPattern.isNotEmpty() }

        for (name in allPropertyNames) {
            var value = getProperty(name) ?: continue

            if (regex?.matches(name) == true) {
                value = "**********"
            }
            // TODO(brian): which logger to use here?
            println("$name = $value")
        }
    }

    override fun getConfigurationFilename(): String =
		throw Exception("Unsupported")

    override fun getScHomeDirLocation(): String = 
		throw Exception("Unsupported")

    override fun getScHomeDirName(): String = 
		throw Exception("Unsupported")

    override fun addPropertyChangeListener(listener: PropertyChangeListener?) =
        throw Exception("Unsupported")

    override fun addPropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) =
        throw Exception("Unsupported")

    override fun addVetoableChangeListener(listener: ConfigVetoableChangeListener?) = 
		throw Exception("Unsupported")

    override fun addVetoableChangeListener(propertyName: String?, listener: ConfigVetoableChangeListener?) = 
		throw Exception("Unsupported")

    override fun removePropertyChangeListener(listener: PropertyChangeListener?) = 
		throw Exception("Unsupported")

    override fun removePropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) = 
		throw Exception("Unsupported")

    override fun removeVetoableChangeListener(listener: ConfigVetoableChangeListener?) = 
		throw Exception("Unsupported")

    override fun removeVetoableChangeListener(propertyName: String?, listener: ConfigVetoableChangeListener?) = 
		throw Exception("Unsupported")

    override fun purgeStoredConfiguration() = 
		throw Exception("Unsupported")

    override fun storeConfiguration() = 
		throw Exception("Unsupported")

    override fun setProperties(properties: MutableMap<String, Any>?) = 
		throw Exception("Unsupported")

    override fun setProperty(propertyName: String?, property: Any?) = 
		throw Exception("Unsupported")

    override fun setProperty(propertyName: String?, property: Any?, isSystem: Boolean) = 
		throw Exception("Unsupported")

    override fun removeProperty(propertyName: String?) = 
		throw Exception("Unsupported")

    override fun getPropertyNamesBySuffix(suffix: String?): MutableList<String> = 
		throw Exception("Unsupported")
}
