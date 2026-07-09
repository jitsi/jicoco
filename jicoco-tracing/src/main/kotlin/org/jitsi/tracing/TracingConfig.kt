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
            "tracing.service-name".from(JitsiConfig.newConfig)
        }
        val otlpEndpoint: String by config {
            "tracing.otlp-endpoint".from(JitsiConfig.newConfig)
        }
        val otlpProtocol: String by config {
            "tracing.otlp-protocol".from(JitsiConfig.newConfig)
        }
    }
}
