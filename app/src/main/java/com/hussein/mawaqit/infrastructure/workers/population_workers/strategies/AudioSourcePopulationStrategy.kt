package com.hussein.mawaqit.infrastructure.workers.population_workers.strategies

import android.util.Log
import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.data.remote.dto.AudioSourceDto
import com.hussein.mawaqit.infrastructure.workers.population_workers.DataPopulationStrategy
import io.ktor.client.call.body
import io.ktor.client.request.get

class AudioSourcePopulationStrategy(
    private val db: AppDatabase
) : DataPopulationStrategy {

    override val name: String = "AudioSource"

    override suspend fun shouldPopulate(): Boolean {
        return db.audioSourceDao().count() == 0
    }

    override suspend fun execute(onProgress: suspend (Float) -> Unit) {
        try {
            Log.d(TAG, "Fetching Audio Sources from remote API: $AUDIO_SOURCES_URL")
            val audioSourcesDto: List<AudioSourceDto> = RemoteService.getClient().get(AUDIO_SOURCES_URL).body()

            val audioSources = audioSourcesDto.map { dto ->
                AudioSourceEntity(
                    id = dto.id,
                    name = dto.nameAr ?: dto.nameEn ?: "Unknown",
                    imageUrl = dto.imageUrl,
                    language = dto.language,
                    ayahBaseUrl = dto.sources?.ayah?.baseUrl,
                    surahBaseUrl = dto.sources?.surah?.baseUrl,
                    radioStreamUrl = dto.sources?.radio?.url
                )
            }

            db.audioSourceDao().insertAll(audioSources)
            onProgress(1.0f)
            Log.d(TAG, "Population complete: ${audioSources.size} audio sources")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate Audio Sources", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "AudioSourcePopulation"
        private const val AUDIO_SOURCES_URL = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/audio/sources.json"
    }
}
