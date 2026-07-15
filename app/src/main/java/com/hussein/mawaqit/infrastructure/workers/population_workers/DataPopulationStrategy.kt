package com.hussein.mawaqit.infrastructure.workers.population_workers

interface DataPopulationStrategy {
    val name: String
    suspend fun shouldPopulate(): Boolean
    suspend fun execute(onProgress: suspend (Float) -> Unit)
}
