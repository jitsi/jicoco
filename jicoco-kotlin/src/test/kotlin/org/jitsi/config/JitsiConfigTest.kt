package org.jitsi.config

import io.kotlintest.IsolationMode
import io.kotlintest.Spec
import io.kotlintest.extensions.TopLevelTest
import org.jitsi.utils.config.ConfigSource
import org.jitsi.videobridge.testutils.ConfigSourceWrapper

/**
 * We can't put this class in jicoco-test-kotlin, because it has to access
 * [JitsiConfigFactory] (located in jicoco-kotlin), so it would create a
 * circular dependency.
 */
abstract class JitsiConfigTest : ConfigTest() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf
    private val legacyConfigWrapper = ConfigSourceWrapper()
    private val newConfigWrapper = ConfigSourceWrapper()

    override fun withLegacyConfig(configSource: ConfigSource) {
        legacyConfigWrapper.innerConfig = configSource
    }

    override fun withNewConfig(configSource: ConfigSource) {
        newConfigWrapper.innerConfig = configSource
    }

    override fun beforeSpecClass(spec: Spec, tests: List<TopLevelTest>) {
        super.beforeSpecClass(spec, tests)
        JitsiConfigFactory.legacyConfigSupplier = { legacyConfigWrapper }
        JitsiConfigFactory.newConfigSupplier = { newConfigWrapper }
    }
}
