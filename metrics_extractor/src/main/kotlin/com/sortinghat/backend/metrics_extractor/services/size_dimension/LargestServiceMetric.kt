package com.sortinghat.backend.metrics_extractor.services.size_dimension

import com.sortinghat.backend.domain.behaviors.Visitor
import com.sortinghat.backend.domain.behaviors.VisitorBag
import com.sortinghat.backend.domain.model.Service
import com.sortinghat.backend.metrics_extractor.services.MetricExtractor
import com.sortinghat.backend.metrics_extractor.vo.ValueResult

class LargestServiceMetric(
    private val visitorBag: VisitorBag = VisitorBag()
) : MetricExtractor, Visitor by visitorBag {

    private val services = mutableSetOf<Service>()
    private var maxNumberOfOperations = 0

    override fun getResult(): ValueResult {
        val largestServices = services
            .fold(setOf<Service>()) { set, service ->
                val numberOfOperations = service.exposedOperations.size
                if (numberOfOperations == maxNumberOfOperations) {
                    set.plus(service)
                } else {
                    set
                }
            }
            .sortedBy { service -> service.name }
            .map { service -> service.name }

        return ValueResult(value = largestServices)
    }

    override fun getMetricDescription(): String {
        return "Largest service ($maxNumberOfOperations ${
            if (maxNumberOfOperations == 1) "operation" else "operations"
        })"
    }

    override fun visit(s: Service) {
        if (s in visitorBag.visited) return

        val numberOfOperations = s.exposedOperations.size
        if (numberOfOperations > maxNumberOfOperations) {
            maxNumberOfOperations = numberOfOperations
        }

        services.add(s)
        visitorBag.addVisited(s)
        s.children().forEach { it.accept(this) }
    }
}
