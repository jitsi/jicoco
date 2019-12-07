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
        println("setting inner config on new config instance ${newConfigWrapper.hashCode()}")
        newConfigWrapper.innerConfig = configSource
    }

    protected companion object {
        private val legacyConfigWrapper = ConfigSourceWrapper().also {
            JitsiConfigFactory.legacyConfigSupplier = {
                println("in the test legacy config supplier!")
                it
            }
        }
        private val newConfigWrapper = ConfigSourceWrapper().also {
            JitsiConfigFactory.newConfigSupplier = {
                println("in the test new config supplier! returning instance ${it.hashCode()}")
                it
            }
        }
    }
}