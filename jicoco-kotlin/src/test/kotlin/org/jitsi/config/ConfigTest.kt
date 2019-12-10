package org.jitsi.config

import io.kotlintest.IsolationMode
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.config.ConfigSource
import org.jitsi.videobridge.testutils.ConfigSourceWrapper

abstract class ConfigTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    protected fun withLegacyConfig(configSource: ConfigSource) {
        legacyConfigWrapper.innerConfig = configSource
    }

    protected fun withNewConfig(configSource: ConfigSource) {
        newConfigWrapper.innerConfig = configSource
    }

    protected companion object {
        private val legacyConfigWrapper = ConfigSourceWrapper().also {
            JitsiConfigFactory.legacyConfigSupplier = { it }
        }
        private val newConfigWrapper = ConfigSourceWrapper().also {
            JitsiConfigFactory.newConfigSupplier = { it }
        }
    }
}
