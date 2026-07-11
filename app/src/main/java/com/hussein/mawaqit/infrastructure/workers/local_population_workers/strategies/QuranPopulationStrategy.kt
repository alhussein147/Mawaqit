package com.hussein.mawaqit.infrastructure.workers.local_population_workers.strategies

import android.util.Log
import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.data.remote.dto.QuranSurahDto
import com.hussein.mawaqit.infrastructure.workers.local_population_workers.DataPopulationStrategy
import io.ktor.client.call.body
import io.ktor.client.request.get

class QuranPopulationStrategy(
    private val db: AppDatabase
) : DataPopulationStrategy {

    override val name: String = "Quran"

    override suspend fun shouldPopulate(): Boolean {
        return db.ayahDao().count() == 0
    }

    override suspend fun execute(onProgress: suspend (Float) -> Unit) {
        try {
            Log.d(TAG, "Fetching Quran from remote API: $QURAN_URL")
            val surahsDto: List<QuranSurahDto> = RemoteService.getClient().get(QURAN_URL).body()

            val surahs = mutableListOf<SurahEntity>()
            val ayahs = mutableListOf<AyahEntity>()
            var lastProgress = -1f

            surahsDto.forEachIndexed { index, surahDto ->
                surahs.add(
                    SurahEntity(
                        number = surahDto.id,
                        nameArabic = surahDto.name,
                        transliteration = surahDto.transliteration,
                        totalAyahs = surahDto.verses.size,
                        origin = surahDto.type
                    )
                )

                surahDto.verses.forEach { ayahDto ->
                    ayahs.add(
                        AyahEntity(
                            surahNumber = surahDto.id,
                            numberInSurah = ayahDto.id,
                            text = ayahDto.text,
                            normalizedText = ayahDto.normalized
                        )
                    )
                }

                val progress = (index + 1) / 114f
                if (progress - lastProgress >= 0.05f || index == 113) {
                    lastProgress = progress
                    onProgress(progress)
                }
            }

            db.surahDao().insertAll(surahs)
            db.ayahDao().insertAll(ayahs)
            
            Log.d(TAG, "Population complete: ${surahs.size} surahs, ${ayahs.size} ayahs")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to populate Quran", e)
            throw e
        }
    }

    companion object {
        private const val TAG = "QuranPopulation"
        private const val QURAN_URL = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/quran/quran.json"
    }
}
