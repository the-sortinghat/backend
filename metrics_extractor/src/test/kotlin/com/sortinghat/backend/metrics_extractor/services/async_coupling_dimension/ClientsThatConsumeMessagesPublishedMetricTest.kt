package com.sortinghat.backend.metrics_extractor.services.async_coupling_dimension

import com.sortinghat.backend.metrics_extractor.vo.PerComponentResult
import com.sortinghat.backend.domain.model.*
import com.sortinghat.backend.metrics_extractor.services.ServicesBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ClientsThatConsumeMessagesPublishedMetricTest {

    @Test
    fun `should return 0 for all services and modules when there is no async communications`() {
        val services = ServicesBuilder().build()
        val metricExtractor = ClientsThatConsumeMessagesPublishedMetric()
        val expected = PerComponentResult(
            modules = services.groupBy { it.module }.keys.associateWith { 0 }.mapKeys { it.key.name },
            services = services.associateWith { 0 }.mapKeys { it.key.name }
        )

        services.forEach { service -> service.accept(metricExtractor) }

        assertEquals(expected, metricExtractor.getResult())
    }

    @Test
    fun `should compute the number of different clients that consume messages published by every service`() {
        val services = ServicesBuilder().build()

        services[0].publishTo(MessageChannel("Topic1"))
        services[0].publishTo(MessageChannel("Topic2"))
        services[1].publishTo(MessageChannel("Topic3"))
        services[0].publishTo(MessageChannel("Topic4"))
        services[1].subscribeTo(MessageChannel("Topic1"))
        services[2].subscribeTo(MessageChannel("Topic2"))
        services[2].subscribeTo(MessageChannel("Topic3"))
        services[1].subscribeTo(MessageChannel("Topic4"))

        val expected = mapOf(
            services[0].name to 2,
            services[1].name to 1,
            services[2].name to 0
        )

        val metricExtractor = ClientsThatConsumeMessagesPublishedMetric()

        services.forEach { service -> service.accept(metricExtractor) }

        val actual = metricExtractor.getResult().services

        assertEquals(expected, actual)
    }

    @Test
    fun `should compute the number of different clients that consume messages published by every module`() {
        val services = ServicesBuilder().build()

        val modules = services.groupBy { it.module }.keys.toList()

        services[0].publishTo(MessageChannel("Topic1"))
        services[0].publishTo(MessageChannel("Topic2"))
        services[1].publishTo(MessageChannel("Topic3"))
        services[0].publishTo(MessageChannel("Topic4"))
        services[1].subscribeTo(MessageChannel("Topic1"))
        services[2].subscribeTo(MessageChannel("Topic2"))
        services[2].subscribeTo(MessageChannel("Topic3"))
        services[1].subscribeTo(MessageChannel("Topic4"))

        val expected = mapOf(
            modules[0].name to 2,
            modules[1].name to 1,
            modules[2].name to 0
        )

        val metricExtractor = ClientsThatConsumeMessagesPublishedMetric()

        services.forEach { service -> service.accept(metricExtractor) }

        val actual = metricExtractor.getResult().modules

        assertEquals(expected, actual)
    }

    @Test
    fun `should not compute async messages between services in the same module`() {
        val services = ServicesBuilder().build()
        services.add(
            Service(
                name = "Data Collector Outro",
                responsibility = "",
                module = Module("Data Collector"),
                system = ServiceBasedSystem(name = "InterSCity", description = "InterSCity")
            )
        )

        val modules = services.groupBy { it.module }.keys.toList()

        services[0].publishTo(MessageChannel("Topic1"))
        services[0].publishTo(MessageChannel("Topic2"))
        services[1].publishTo(MessageChannel("Topic3"))
        services[1].publishTo(MessageChannel("Topic4"))
        services[2].publishTo(MessageChannel("Topic5"))
        services[3].publishTo(MessageChannel("Topic6"))
        services[1].subscribeTo(MessageChannel("Topic1"))
        services[3].subscribeTo(MessageChannel("Topic2"))
        services[2].subscribeTo(MessageChannel("Topic3"))
        services[3].subscribeTo(MessageChannel("Topic4"))
        services[3].subscribeTo(MessageChannel("Topic5"))
        services[2].subscribeTo(MessageChannel("Topic6"))

        val expected = mapOf(
            modules[0].name to 2,
            modules[1].name to 1,
            modules[2].name to 0
        )

        val metricExtractor = ClientsThatConsumeMessagesPublishedMetric()

        services.forEach { service -> service.accept(metricExtractor) }

        val actual = metricExtractor.getResult().modules

        assertEquals(expected, actual)
    }
}
