package org.jitsi.config

import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigObject
import org.jitsi.service.configuration.ConfigVetoableChangeListener
import org.jitsi.service.configuration.ConfigurationService
import java.beans.PropertyChangeListener

/**
 * This class serves as a shim implementation of [ConfigurationService]
 * which reads the legacy config file but via the new config
 * implementation.  Note that not all functionality is implemented by
 * this shim: only those methods which were found to be used by code
 * relying on this shim.
 */
class LegacyConfigurationServiceShim : ConfigurationService {

    private fun <T> getOrDefault(default: T, block: () -> T): T {
        return try {
            block()
        } catch (e: ConfigException.Missing) {
            default
        } catch (e: ConfigException.WrongType) {
            // The old code sometimes checks for a path such as:
            // org.jitsi.videobridge.STATISTICS_INTERVAL.muc
            // which doesn't exist, but the path
            // org.jitsi.videobridge.STATISTICS_INTERVAL
            // *does* exist.  This results in a WrongType exception
            // (since, when walking the config looking for
            // org.jitsi.videobridge.STATISTICS_INTERVAL.muc,
            // we expect the value at
            // org.jitsi.videobridge.STATISTICS_INTERVAL to be an
            // object, but instead it ends up being a value).  So we return
            // the default value here.
            default
        }
    }

    override fun getBoolean(path: String?, defaultValue: Boolean): Boolean {
        path ?: return defaultValue
        return getOrDefault(defaultValue) {
            JitsiConfig.legacyConfig.getterFor(Boolean::class)(path)
        }
    }

    override fun getDouble(path: String?, defaultValue: Double): Double {
        path ?: return defaultValue
        return getOrDefault(defaultValue) {
            JitsiConfig.legacyConfig.getterFor(Double::class)(path)
        }
    }

    override fun getInt(path: String?, defaultValue: Int): Int {
        path ?: return defaultValue
        return getOrDefault(defaultValue) {
            JitsiConfig.legacyConfig.getterFor(Int::class)(path)
        }
    }

    override fun getLong(path: String?, defaultValue: Long): Long {
        path ?: return defaultValue
        return getOrDefault(defaultValue) {
            JitsiConfig.legacyConfig.getterFor(Long::class)(path)
        }
    }

    override fun getString(path: String?): String? =
        getString(path, null)

    override fun getString(path: String?, defaultValue: String?): String? {
        path ?: return defaultValue
        return getOrDefault(defaultValue) {
            JitsiConfig.legacyConfig.getterFor(String::class)(path)
        }
    }

    override fun getProperty(path: String?): Any? {
        path ?: return null
        return getOrDefault(null) {
            JitsiConfig.legacyConfig.getterFor(ConfigObject::class)(path)
        }
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun addPropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun addVetoableChangeListener(listener: ConfigVetoableChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun addVetoableChangeListener(propertyName: String?, listener: ConfigVetoableChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun removePropertyChangeListener(propertyName: String?, listener: PropertyChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun removeVetoableChangeListener(listener: ConfigVetoableChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun removeVetoableChangeListener(propertyName: String?, listener: ConfigVetoableChangeListener?) {
        throw Exception("Unsupported")
    }

    override fun getAllPropertyNames(): MutableList<String> {
        throw Exception("Unsupported")
    }

    override fun getPropertyNamesByPrefix(prefix: String?, exactPrefixMatch: Boolean): MutableList<String> {
        prefix ?: return mutableListOf()
        return getOrDefault(mutableListOf()) {
            val obj = JitsiConfig.legacyConfig.getterFor(ConfigObject::class)(prefix)
            obj.toConfig().entrySet()
                    .map { it.key }
                    .filter { propName ->
                        if (exactPrefixMatch) {
                            // If an exact prefix match was requested, don't
                            // include keys nested under further scopes
                            !propName.contains(".")
                        }
                        true
                    }
                    .map { "$prefix.$it"}
                    .toMutableList()
        }
    }

    override fun getPropertyNamesBySuffix(suffix: String?): MutableList<String> {
        throw Exception("Unsupported")
    }

    override fun logConfigurationProperties(excludePatter: String?) {
        // Leaving this as a no-op, we already log the config
    }

    override fun reloadConfiguration() {
        // Should we support calling reload here? Anything using this?
        throw Exception("Unsupported")
    }

    override fun removeProperty(propertyName: String?) {
        throw Exception("Unsupported")
    }

    override fun setProperties(properties: MutableMap<String, Any>?) {
        throw Exception("Unsupported")
    }

    override fun setProperty(propertyName: String?, value: Any?) {
        throw Exception("Unsupported")
    }

    override fun setProperty(propertyName: String?, value: Any?, isSystem: Boolean) {
        throw Exception("Unsupported")
    }

    override fun purgeStoredConfiguration() {
        throw Exception("Unsupported")
    }

    override fun storeConfiguration() {
        throw Exception("Unsupported")
    }

    override fun getConfigurationFilename(): String {
        throw Exception("Unsupported")
    }

    override fun getScHomeDirLocation(): String {
        throw Exception("Unsupported")
    }

    override fun getScHomeDirName(): String {
        throw Exception("Unsupported")
    }
}