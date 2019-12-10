package org.jitsi.config

import io.kotlintest.IsolationMode
import org.jitsi.utils.config.ConfigSource
import org.jitsi.videobridge.testutils.ConfigSourceWrapper

/**
 * We can't put this class in jicoco-test-kotlin, because it has to access
 * [JitsiConfigFactory] (located in jicoco-kotlin), so it would create a
 * circular dependency.
 */
abstract class JitsiConfigTest : ConfigTest() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    override fun withLegacyConfig(configSource: ConfigSource) {
        legacyConfigWrapper.innerConfig = configSource
    }

    override fun withNewConfig(configSource: ConfigSource) {
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
