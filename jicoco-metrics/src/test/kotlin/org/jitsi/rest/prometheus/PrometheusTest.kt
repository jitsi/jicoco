package org.jitsi.rest.prometheus

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.prometheus.client.CollectorRegistry
import io.prometheus.client.exporter.common.TextFormat
import jakarta.ws.rs.core.Application
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.jetty.http.HttpStatus
import org.glassfish.jersey.server.ResourceConfig
import org.glassfish.jersey.test.JerseyTest
import org.glassfish.jersey.test.TestProperties
import org.jitsi.metrics.MetricsContainer
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.junit.Test

class PrometheusTest : JerseyTest() {
    private lateinit var metricsContainer: MetricsContainer
    private val baseUrl = "/metrics"
    private val mediaType004 = MediaType("text", "plain", mapOf("charset" to "utf-8", "version" to "0.0.4"))
    private val mediaTypeOpenMetrics =
        MediaType("application", "openmetrics-text", mapOf("charset" to "utf-8", "version" to "1.0.0"))

    override fun configure(): Application {
        metricsContainer = MetricsContainer(CollectorRegistry()).apply {
            checkForNameConflicts = false
            registerLongGauge("gauge", "A gauge", 50)
            registerBooleanMetric("boolean", "A boolean", true)
        }

        enable(TestProperties.LOG_TRAFFIC)
        enable(TestProperties.DUMP_ENTITY)
        return object : ResourceConfig() {
            init {
                register(Prometheus(metricsContainer))
            }
        }
    }

    @Test
    fun testGetJson() {
        val resp = target(baseUrl).request(MediaType.APPLICATION_JSON_TYPE).get()
        resp.status shouldBe HttpStatus.OK_200
        resp.mediaType shouldBe MediaType.APPLICATION_JSON_TYPE
        with(MetricsContainer(CollectorRegistry())) {
            registerLongGauge("gauge", "A gauge", 50)
            registerBooleanMetric("boolean", "A boolean", true)
            resp.getResultAsJson() shouldBe mapOf("gauge" to 50, "boolean" to true)
        }
    }

    @Test
    fun testGetPrometheus004() {
        val resp = target(baseUrl).request(TextFormat.CONTENT_TYPE_004).get()
        resp.status shouldBe HttpStatus.OK_200
        resp.mediaType shouldBe mediaType004
        with(MetricsContainer(CollectorRegistry())) {
            registerLongGauge("gauge", "A gauge", 50)
            registerBooleanMetric("boolean", "A boolean", true)
            val expected = getPrometheusMetrics(TextFormat.CONTENT_TYPE_004).split("\n").sorted()
            resp.getSortedLines() shouldBe expected
        }
    }

    @Test
    fun testGetPrometheusOpenMetrics() {
        val resp = target(baseUrl).request(TextFormat.CONTENT_TYPE_OPENMETRICS_100).get()
        resp.status shouldBe HttpStatus.OK_200
        resp.mediaType shouldBe mediaTypeOpenMetrics
        with(MetricsContainer(CollectorRegistry())) {
            registerLongGauge("gauge", "A gauge", 50)
            registerBooleanMetric("boolean", "A boolean", true)
            val expected = getPrometheusMetrics(TextFormat.CONTENT_TYPE_OPENMETRICS_100).split("\n").sorted()
            resp.getSortedLines() shouldBe expected
        }
    }

    private fun Response.getResultAsJson(): JSONObject {
        val obj = JSONParser().parse(readEntity(String::class.java))
        obj.shouldBeInstanceOf<JSONObject>()
        return obj
    }

    private fun Response.getSortedLines(): List<String> {
        return readEntity(String::class.java).split("\n").sorted()
    }
}
