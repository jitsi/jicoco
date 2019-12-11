package org.jitsi.config

import org.jitsi.utils.config.ConfigSource

/**
 * A factory for instantiating the various config types.  Used as a hook
 * for testing.
 */
class JitsiConfigFactory {
    companion object {
        var newConfigSupplier: () -> ConfigSource = { NewConfig() }
        var legacyConfigSupplier: () -> ConfigSource = { LegacyConfig() }
        var legacyConfigurationServiceShimSupplier: () -> ExpandedConfigurationService = { LegacyConfigurationServiceShim() }
    }
}
