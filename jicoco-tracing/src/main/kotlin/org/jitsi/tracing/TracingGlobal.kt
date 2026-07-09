package org.jitsi.tracing

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor

class TracingGlobal {
    companion object {
        val sdk: OpenTelemetry by lazy {
            if (!TracingConfig.enabled) {
                return@lazy OpenTelemetry.noop()
            }

            val exporter = when (TracingConfig.otlpProtocol) {
                "grpc" -> {
                    OtlpGrpcSpanExporter.builder().setEndpoint(TracingConfig.otlpEndpoint).build()
                }

                "http" -> {
                    OtlpHttpSpanExporter.builder().setEndpoint(TracingConfig.otlpEndpoint).build()
                }

                else -> {
                    throw Exception("unknown otlp protocol")
                }
            }

            val tracer = SdkTracerProvider.builder().addResource(
                Resource.builder().put("service.name", TracingConfig.serviceName).build()
            ).addSpanProcessor(
                BatchSpanProcessor.builder(exporter).build()
            ).build()

            OpenTelemetrySdk.builder().setTracerProvider(tracer).build()
        }
    }
}
