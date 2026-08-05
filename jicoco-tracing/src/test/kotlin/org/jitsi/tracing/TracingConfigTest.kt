/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.tracing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import org.jitsi.config.withNewConfig
import org.jitsi.metaconfig.ConfigException

class TracingConfigTest : ShouldSpec() {
    init {
        context("enabled") {
            should("default to false") {
                TracingConfig.enabled shouldBe false
            }
            should("read from config") {
                withNewConfig("tracing.enabled=true") {
                    TracingConfig.enabled shouldBe true
                }
            }
        }

        context("when tracing is disabled") {
            should("not require serviceName, otlpEndpoint or otlpProtocol to be set") {
                shouldThrow<ConfigException.UnableToRetrieve.ConditionNotMet> { TracingConfig.serviceName }
                shouldThrow<ConfigException.UnableToRetrieve.ConditionNotMet> { TracingConfig.otlpEndpoint }
                shouldThrow<ConfigException.UnableToRetrieve.ConditionNotMet> { TracingConfig.otlpProtocol }
            }
        }

        context("when tracing is enabled") {
            val config = """
                tracing.enabled=true
                tracing.service-name=test-service
                tracing.otlp-endpoint="http://localhost:4317"
                tracing.otlp-protocol=grpc
            """.trimIndent()

            should("read serviceName, otlpEndpoint and otlpProtocol from config") {
                withNewConfig(config) {
                    TracingConfig.serviceName shouldBe "test-service"
                    TracingConfig.otlpEndpoint shouldBe "http://localhost:4317"
                    TracingConfig.otlpProtocol shouldBe "grpc"
                }
            }
        }
    }
}
