package com.hussein.mawaqit.infrastructure.workers.local_population_workers.strategies

import android.util.Log
import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.data.remote.dto.TafsirDto
import com.hussein.mawaqit.infrastructure.workers.local_population_workers.DataPopulationStrategy
import io.ktor.client.call.body
import io.ktor.client.request.get

class TafsirPopulationStrategy(
    private val db: AppDatabase,
    private val source: TafsirSourceEntity
) : DataPopulationStrategy {

    override val name: String = "Tafsir_${source.id}"

    override suspend fun shouldPopulate(): Boolean {
        val isDownloaded = db.tafsirDao().getSourceById(source.id)?.downloaded ?: false
        return !isDownloaded
    }

    override suspend fun execute(onProgress: suspend (Float) -> Unit) {
        try {
            Log.d(TAG, "Fetching Tafsir from remote API: ${source.url}")
            val tafsirs: List<TafsirDto> = RemoteService.getClient().get(source.url).body()

            db.tafsirDao().deleteTafsirBySource(source.id)

            val total = tafsirs.size
            val batch = mutableListOf<TafsirEntity>()
            var processed = 0

            tafsirs.forEach { dto ->
                batch += TafsirEntity(
                    sourceId = source.id,
                    surahNumber = dto.surah,
                    numberInSurah = dto.ayahNumber,
                    text = dto.text
                )
                processed++

                if (batch.size >= BATCH_SIZE) {
                    db.tafsirDao().insertAll(batch)
                    batch.clear()
                    onProgress(processed.toFloat() / total)
                }
            }

            if (batch.isNotEmpty()) {
                db.tafsirDao().insertAll(batch)
            }

            db.tafsirDao().updateSourceDownloaded(source.id, true)

            onProgress(1f)
            Log.d(TAG, "Population complete: $processed entries")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate tafsir", e)
            throw e
        }
    }


    companion object {
        private const val TAG = "TafsirPopulation"
        private const val BATCH_SIZE = 500
    }
}
