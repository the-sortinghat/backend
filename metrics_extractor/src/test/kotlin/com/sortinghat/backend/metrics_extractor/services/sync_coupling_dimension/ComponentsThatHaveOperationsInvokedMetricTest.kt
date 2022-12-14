package com.sortinghat.backend.metrics_extractor.services.sync_coupling_dimension

import com.sortinghat.backend.domain.model.Module
import com.sortinghat.backend.domain.model.Operation
import com.sortinghat.backend.domain.model.Service
import com.sortinghat.backend.domain.model.ServiceBasedSystem
import com.sortinghat.backend.metrics_extractor.services.ServicesBuilder
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ComponentsThatHaveOperationsInvokedMetricTest {

    @Test
    fun `should compute the number of different services from which a given service invokes operations`() {
        val services = ServicesBuilder().build()

        services[0].expose(Operation.fromString("GET /users"))
        services[1].expose(Operation.fromString("POST /users"))
        services[1].expose(Operation.fromString("GET /users/{id}"))
        services[1].consume(Operation.fromString("GET /users"))
        services[2].consume(Operation.fromString("POST /users"))
        services[2].consume(Operation.fromString("GET /users/{id}"))

        val expected = mapOf(
            services[0].name to 0,
            services[1].name to 1,
            services[2].name to 1
        )

        val metricExtractor = ComponentsThatHaveOperationsInvokedMetric()

        services.forEach { s -> s.accept(metricExtractor) }

        val actual = metricExtractor.getResult().services

        assertEquals(expected, actual)
    }

    @Test
    fun `should compute the number of different modules from which a given module invokes operations`() {
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

        services[0].expose(Operation.fromString("GET /users"))
        services[0].expose(Operation.fromString("PUT /users/{id}"))
        services[1].expose(Operation.fromString("POST /users"))
        services[1].expose(Operation.fromString("GET /users/{id}"))
        services[2].expose(Operation.fromString("DELETE /users/{id}"))
        services[3].expose(Operation.fromString("GET /foo"))
        services[1].consume(Operation.fromString("GET /users"))
        services[3].consume(Operation.fromString("PUT /users/{id}"))
        services[2].consume(Operation.fromString("POST /users"))
        services[3].consume(Operation.fromString("GET /users/{id}"))
        services[3].consume(Operation.fromString("DELETE /users/{id}"))
        services[2].consume(Operation.fromString("GET /foo"))

        val expected = mapOf(
            modules[0].name to 0,
            modules[1].name to 1,
            modules[2].name to 2
        )

        val metricExtractor = ComponentsThatHaveOperationsInvokedMetric()

        services.forEach { s -> s.accept(metricExtractor) }

        val actual = metricExtractor.getResult().modules

        assertEquals(expected, actual)
    }
}
