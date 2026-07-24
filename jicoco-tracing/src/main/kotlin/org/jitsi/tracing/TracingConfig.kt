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

import org.jitsi.config.JitsiConfig
import org.jitsi.metaconfig.config

class TracingConfig {
    companion object {
        val enabled: Boolean by config {
            "tracing.enabled".from(JitsiConfig.newConfig)
            "default" { false }
        }
        val serviceName: String by config {
            onlyIf("tracing is enabled", ::enabled) {
                "tracing.service-name".from(JitsiConfig.newConfig)
            }
        }
        val otlpEndpoint: String by config {
            onlyIf("tracing is enabled", ::enabled) {
                "tracing.otlp-endpoint".from(JitsiConfig.newConfig)
            }
        }
        val otlpProtocol: String by config {
            onlyIf("tracing is enabled", ::enabled) {
                "tracing.otlp-protocol".from(JitsiConfig.newConfig)
            }
        }
    }
}
