package org.jitsi.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import org.jitsi.utils.ConfigUtils
import java.util.regex.Pattern

private fun shouldMask(pattern: Pattern?, key: String): Boolean {
    pattern ?: return false
    return pattern.matcher(key).find()
}

const val MASK = "******"

/**
 * Returns a new [Config] with any values matching
 * [ConfigUtils.PASSWORD_SYS_PROPS] changed to [MASK].   NOTE
 * that this will change the type of the value to a [String]
 */
fun Config.mask(): Config {
    // Or should this be held in the config itself?
    val fieldRegex = ConfigUtils.PASSWORD_SYS_PROPS ?: return ConfigFactory.load(this)
    val pattern =
        Pattern.compile(fieldRegex, Pattern.CASE_INSENSITIVE)
    return entrySet().fold(this) { config, (key, _) ->
        when {
            shouldMask(pattern, key) -> config.withValue(key, ConfigValueFactory.fromAnyRef(MASK))
            else -> config
        }
    }
}

/**
 * For the legacy config service shim we need to check both
 * the properties in the legacy config file and the system properties, but
 * there is no way to parse only the system properties so we have to load
 * them all, and then filter out properties that came from places other
 * than the system properties.
 *
 * Unfortunately, the only way to do this is to filter based on the description
 * in the origin.
 *
 * @return a new [Config] with only values coming from the origin described
 * by [originDescription].
 */
fun Config.withOnlyOrigin(originDescription: String): Config {
    return entrySet().fold(ConfigFactory.parseString("")) { config, (key, value) ->
        when {
            value.origin().description().equals(originDescription) ->
                config.withValue(key, value)
            else -> config
        }
    }
}