/*
 * Copyright @ 2020 - present 8x8, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jitsi.config.SystemPropertiesConfigSource
import org.jitsi.metaconfig.ConfigException
import kotlin.reflect.typeOf

class AbstractReadOnlyConfigurationServiceTest : ShouldSpec() {
    override fun isolationMode(): IsolationMode? = IsolationMode.InstancePerLeaf

    private val config = SystemPropertiesConfigSource()

    init {
        context("Retrieving values") {
            context("Boolean") {
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Int>("boolean") }

                System.setProperty("boolean", "true")
                config.getValue<Boolean>("boolean") shouldBe true

                System.setProperty("boolean", "false")
                config.getValue<Boolean>("boolean") shouldBe false

                System.setProperty("boolean", "something else")
                config.getValue<Boolean>("boolean") shouldBe false

                System.clearProperty("boolean")
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Int>("boolean") }
            }
            context("Int") {
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Int>("integer") }

                System.setProperty("integer", "1")
                config.getValue<Int>("integer") shouldBe 1

                System.setProperty("integer", "not an int")
                shouldThrow<ConfigException.UnableToRetrieve.WrongType> { config.getValue<Int>("integer") }

                System.clearProperty("integer")
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Int>("integer") }
            }
            context("Long") {
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Long>("long") }

                System.setProperty("long", "1")
                config.getValue<Long>("long") shouldBe 1

                System.setProperty("long", "not an int")
                shouldThrow<ConfigException.UnableToRetrieve.WrongType> { config.getValue<Long>("long") }

                System.clearProperty("long")
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Long>("long") }
            }
            context("Double") {
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Double>("double") }

                System.setProperty("double", "1")
                config.getValue<Double>("double") shouldBe 1

                System.setProperty("double", "1.1")
                config.getValue<Double>("double") shouldBe 1.1

                System.setProperty("double", "not an int")
                shouldThrow<ConfigException.UnableToRetrieve.WrongType> { config.getValue<Double>("double") }

                System.clearProperty("double")
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<Double>("double") }
            }
            context("String") {
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<String>("string") }

                System.setProperty("string", "1")
                config.getValue<String>("string") shouldBe "1"

                System.setProperty("string", "a string")
                config.getValue<String>("string") shouldBe "a string"

                System.clearProperty("string")
                shouldThrow<ConfigException.UnableToRetrieve.NotFound> { config.getValue<String>("string") }
            }
        }
    }
}

private inline fun <reified T : Any> SystemPropertiesConfigSource.getValue(path: String): T {
    val getter = this.getterFor(typeOf<T>())
    return getter(path) as T
}
