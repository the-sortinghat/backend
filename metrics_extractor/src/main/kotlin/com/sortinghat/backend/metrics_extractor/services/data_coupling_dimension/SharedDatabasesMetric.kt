package com.sortinghat.backend.metrics_extractor.services.data_coupling_dimension

import com.sortinghat.backend.domain.behaviors.Visitor
import com.sortinghat.backend.domain.behaviors.VisitorBag
import com.sortinghat.backend.domain.model.Database
import com.sortinghat.backend.domain.model.Service
import com.sortinghat.backend.metrics_extractor.services.MetricExtractor
import com.sortinghat.backend.metrics_extractor.vo.PerComponentResult

class SharedDatabasesMetric(
    private val visitorBag: VisitorBag = VisitorBag()
) : MetricExtractor, Visitor by visitorBag {

    private val services = mutableSetOf<Service>()
    private val usages = mutableMapOf<Database, Set<Service>>()

    override fun getResult(): PerComponentResult {
        val servicesResult = services.associateWith { 0 }.toMutableMap()
        val modulesResult = services.groupBy { it.module }.keys.associateWith { 0 }.toMutableMap()

        usages.values.forEach { usedBy ->
            if (usedBy.size < 2) return@forEach

            usedBy.forEach { service -> servicesResult.merge(service, 1) { old, new -> old + new } }
            usedBy
                .distinctBy { service -> service.module }
                .forEach { service -> modulesResult.merge(service.module, 1) { old, new -> old + new } }
        }

        return PerComponentResult(
            modules = modulesResult.mapKeys { it.key.name },
            services = servicesResult.mapKeys { it.key.name }
        )
    }

    override fun getMetricDescription(): String {
        return "Number of data sources that each component shares with others"
    }

    override fun visit(s: Service) {
        if (s in visitorBag.visited) return

        visitorBag.addVisited(s)
        services.add(s)
        s.children().forEach { it.accept(this) }
    }

    override fun visit(db: Database) {
        if (db in visitorBag.visited) return

        visitorBag.addVisited(db)
        usages[db] = db.usages()
        db.children().forEach { it.accept(this) }
    }
}
