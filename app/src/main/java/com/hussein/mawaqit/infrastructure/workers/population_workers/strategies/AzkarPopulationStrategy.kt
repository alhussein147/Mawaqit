package com.hussein.mawaqit.infrastructure.workers.population_workers.strategies

import android.util.Log
import com.hussein.mawaqit.data.db.repo.AzkarRepository
import com.hussein.mawaqit.data.db.entities.invocation.AzkarCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.AzkarItemEntity
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.data.remote.dto.AzkarCategoryDto
import com.hussein.mawaqit.infrastructure.workers.population_workers.DataPopulationStrategy
import io.ktor.client.call.body
import io.ktor.client.request.get

class AzkarPopulationStrategy(
    private val repository: AzkarRepository
) : DataPopulationStrategy {

    override val name: String = "Azkar"

    override suspend fun shouldPopulate(): Boolean {
        return repository.countCategories() == 0
    }

    override suspend fun execute(onProgress: suspend (Float) -> Unit) {
        try {
            Log.d(TAG, "Fetching Azkar from remote API: $AZKAR_URL")
            val azkarDto: List<AzkarCategoryDto> = RemoteService.getClient().get(AZKAR_URL).body()

            val categories = mutableListOf<AzkarCategoryEntity>()
            val items = mutableListOf<AzkarItemEntity>()

            azkarDto.forEachIndexed { index, categoryDto ->
                categories.add(
                    AzkarCategoryEntity(
                        id = categoryDto.id,
                        title = categoryDto.title,
                        highlight = categoryDto.highlight
                    )
                )

                categoryDto.content.forEach { itemDto ->
                    items.add(
                        AzkarItemEntity(
                            categoryId = categoryDto.id,
                            zekr = itemDto.zekr,
                            repeat = itemDto.repeat,
                            bless = itemDto.bless
                        )
                    )
                }

                if (azkarDto.isNotEmpty()) {
                    onProgress((index + 1) / azkarDto.size.toFloat())
                }
            }

            repository.saveAzkar(categories, items)
            Log.d(TAG, "Population complete: ${categories.size} categories, ${items.size} items")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate Azkar", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "AzkarPopulation"
        private const val AZKAR_URL = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/azkar/azkar.json"
    }
}
