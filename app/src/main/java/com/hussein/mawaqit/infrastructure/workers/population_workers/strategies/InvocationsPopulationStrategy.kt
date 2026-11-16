package com.hussein.mawaqit.infrastructure.workers.population_workers.strategies

import android.util.Log
import com.hussein.mawaqit.data.db.entities.invocation.InvocationCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.InvocationCategoryType
import com.hussein.mawaqit.data.db.entities.invocation.InvocationEntity
import com.hussein.mawaqit.data.db.repo.AzkarRepository
import com.hussein.mawaqit.data.remote.ApiEndpoints
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.data.remote.dto.InvocationCategoryDto
import com.hussein.mawaqit.infrastructure.workers.population_workers.DataPopulationStrategy
import io.ktor.client.call.body
import io.ktor.client.request.get

class InvocationsPopulationStrategy(
    private val repository: AzkarRepository
) : DataPopulationStrategy {

    override val name: String = "Azkar"

    override suspend fun shouldPopulate(): Boolean {
        return repository.countCategories() == 0
    }

    override suspend fun execute(onProgress: suspend (Float) -> Unit) {
        try {
            Log.d(TAG, "Fetching Azkar from remote API: ${ApiEndpoints.Assets.INVOCATIONS_URL}")
            val invocationCategoryDto: List<InvocationCategoryDto> =
                RemoteService.getClient().get(ApiEndpoints.Assets.INVOCATIONS_URL).body()

            val categories = mutableListOf<InvocationCategoryEntity>()
            val items = mutableListOf<InvocationEntity>()

            invocationCategoryDto.forEachIndexed { index, categoryDto ->
                categories.add(
                    InvocationCategoryEntity(
                        id = categoryDto.id,
                        name = categoryDto.title,
                        type = InvocationCategoryType.valueOf(categoryDto.type),
                        highlight = categoryDto.highlight,
                        sortOrder = categoryDto.sortOrder
                    )
                )

                categoryDto.content.forEach { itemDto ->
                    items.add(
                        InvocationEntity(
                            categoryId = categoryDto.id,
                            content = itemDto.text,
                            benefit = itemDto.bless,
                            repeatCount = itemDto.repeat,
                        )
                    )
                }

                if (invocationCategoryDto.isNotEmpty()) {
                    onProgress((index + 1) / invocationCategoryDto.size.toFloat())
                }
            }

            repository.saveInvocations(categories, items)
            Log.d(TAG, "Population complete: ${categories.size} categories, ${items.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate Azkar", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "INVOCATIONS_POPULATION"
    }
}
