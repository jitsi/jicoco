package org.jitsi.config

class JitsiConfig {
    companion object {
        //TODO: type these to ConfigSource once reload gets moved to interface
        val newConfig = NewConfig()
        val legacyConfig = LegacyConfig()

        fun reload() {
            newConfig.reload()
            legacyConfig.reload()
        }
    }
}