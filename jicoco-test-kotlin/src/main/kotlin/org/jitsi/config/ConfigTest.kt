package org.jitsi.config

import io.kotlintest.IsolationMode
import io.kotlintest.specs.ShouldSpec
import org.jitsi.utils.config.ConfigSource
import java.time.Duration
import java.util.Random

abstract class ConfigTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    abstract fun withLegacyConfig(configSource: ConfigSource)

    abstract fun withNewConfig(configSource: ConfigSource)
}

/**
 * A helper class which contains a value as it should appear when first
 * retrieved from the config source and how it should eventually look
 * as a value in the property itself (which may be transformed in some way
 * from the original config source value).
 */
class MockConfigValue<T : Any, U : Any>(
    val configValue: T, val propValue: U
)

/**
 * Generate random [MockConfigValue]s to be used in tests.  This way we can
 * have a single helper which can test changing config values to validate
 * read-once vs read-every-time
 */
interface MockConfigValueGenerator<ConfigType : Any, PropType : Any> {
    fun gen(): MockConfigValue<ConfigType, PropType>
}

abstract class TransformingMockConfigValueGenerator<ConfigType : Any, PropType : Any>(
    val configTypeGenerator: () -> ConfigType,
    val configValueTransformer: (ConfigType) -> PropType
) : MockConfigValueGenerator<ConfigType, PropType> {
    override fun gen(): MockConfigValue<ConfigType, PropType> {
        return configTypeGenerator().let {
            MockConfigValue(it, configValueTransformer(it))
        }
    }
}

object IntMockConfigValueGenerator : MockConfigValueGenerator<Int, Int> {
    override fun gen(): MockConfigValue<Int, Int> {
        return Random().nextInt().let { MockConfigValue(it, it) }
    }
}

object LongMockConfigValueGenerator : MockConfigValueGenerator<Long, Long> {
    override fun gen(): MockConfigValue<Long, Long> {
        return Random().nextLong().let { MockConfigValue(it, it) }
    }
}

/**
 * We don't use a singleton here because collisions would be too common
 */
class BooleanMockConfigValueGenerator : MockConfigValueGenerator<Boolean, Boolean> {
    private var currValue: Boolean = true

    override fun gen(): MockConfigValue<Boolean, Boolean> {
        return MockConfigValue(currValue, currValue).also {
            currValue = !currValue
        }
    }
}

object DurationToLongMockConfigValueGenerator : TransformingMockConfigValueGenerator<Duration, Long>(
    { Duration.ofMillis(Random().nextLong()) },
    { it.toMillis() }
)
