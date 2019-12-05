package org.jitsi.config

import com.typesafe.config.Config
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
    val pattern =
        Pattern.compile(ConfigUtils.PASSWORD_SYS_PROPS, Pattern.CASE_INSENSITIVE)
    return entrySet().fold(this) { config, (key, _) ->
        when {
            shouldMask(pattern, key) -> config.withValue(key, ConfigValueFactory.fromAnyRef(MASK))
            else -> config
        }
    }
}