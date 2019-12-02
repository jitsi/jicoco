package org.jitsi.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigObject
import com.typesafe.config.ConfigValue
import org.jitsi.utils.config.ConfigSource
import org.jitsi.utils.config.exception.ConfigurationValueTypeUnsupportedException
import java.time.Duration
import java.time.Period
import java.time.temporal.TemporalAmount
import kotlin.reflect.KClass

/**
 * A [ConfigSource] which reads from a [com.typesafe.Config]
 * object.  Subclasses can be created to support parsing types
 * not handled here.
 */
open class TypesafeConfigSource(
    override val name: String,
    private val config: Config
) : ConfigSource {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getterFor(valueType: KClass<T>): (String) -> T {
        return when(valueType) {
            Boolean::class -> { path -> config.getBoolean(path) as T }
            Number::class -> { path -> config.getNumber(path) as T }
            Int::class -> { path -> config.getInt(path) as T }
            Long::class -> { path -> config.getLong(path) as T }
            Double::class -> { path -> config.getDouble(path) as T }
            String::class -> { path -> config.getString(path) as T }
            //TODO: enum...working on how to cast the type correctly
//            Enum::class -> { path ->
//                val c = valueType.java as Class<Enum>
//                config.getEnum(valueType.java as Class<Enum<T>>, path) as T
//            }
            // ConfigObject and ConfigValue are useful when parsing custom
            // or complex types, as we can first retrieve them as this type
            // and then transform them as needed
            ConfigObject::class -> { path -> config.getObject(path) as T }
            ConfigValue::class -> { path -> config.getValue(path) as T }
            Duration::class -> { path -> config.getDuration(path) as T }
            Period::class -> { path -> config.getPeriod(path) as T }
            TemporalAmount::class -> { path -> config.getTemporal(path) as T }
            //TODO: in order to support the getXXXList methods, we'll need to do
            // some more work: we can't pass List<XXX> to this method since
            // we lose the inner generic type (List's generic type), so we won't
            // know which one to call.  Look into the method described here:
            // https://stackoverflow.com/a/37099526 (super class tokens).
            // In the meantime, users can retrieve as ConfigValue and do the
            // transformation manually
            else -> throw ConfigurationValueTypeUnsupportedException.new(valueType)
        }
    }
    //TODO: translate typesafeconfig exceptions(?)
}
