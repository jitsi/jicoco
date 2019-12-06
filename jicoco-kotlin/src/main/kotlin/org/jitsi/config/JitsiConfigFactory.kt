package org.jitsi.config

import org.jitsi.utils.config.ConfigSource

class JitsiConfigFactory {
    companion object {
        var newConfigSupplier: () -> ConfigSource = { NewConfig() }
        var legacyConfigSupplier: () -> ConfigSource = { LegacyConfig() }
        var legacyConfigurationServiceShimSupplier: () -> ExpandedConfigurationService = { LegacyConfigurationServiceShim() }
    }
}
