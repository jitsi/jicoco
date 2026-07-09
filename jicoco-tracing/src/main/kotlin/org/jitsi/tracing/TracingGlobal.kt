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
