package com.sortinghat.backend.metrics_extractor.vo

data class PerComponentResult(val modules: Map<String, Int>, val services: Map<String, Int>) : ExtractionResult
