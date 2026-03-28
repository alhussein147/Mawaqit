package com.hussein.mawaqit.data.infrastructure.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.data.db.AyahEntity
import com.hussein.mawaqit.data.db.QuranDatabase
import com.hussein.mawaqit.data.db.SurahEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class QuranPopulationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val db: QuranDatabase by inject()
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        const val WORK_NAME = "quran_population"
        const val KEY_PROGRESS = "progress"
        const val KEY_CURRENT_SURAH = "current_surah"
        private const val TAG = "QuranPopulation"

    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            if (db.ayahDao().count() > 0) {
                Log.d(TAG, "Database already populated — skipping")
                return@withContext Result.success()
            }

            Log.d(TAG, "Starting Quran database population")

            val surahs = mutableListOf<SurahEntity>()
            val ayahs = mutableListOf<AyahEntity>()

            // Read the whole Quran from a single file
            val raw = applicationContext.assets
                .open("quran/quran.json")
                .bufferedReader()
                .use { it.readText() }
            val surahsArray = json.parseToJsonElement(raw).jsonArray

            surahsArray.forEachIndexed { index, surahElement ->
                val root = surahElement.jsonObject
                val surahNumber = root["id"]!!.jsonPrimitive.content.toInt()
                val versesArray = root["verses"]!!.jsonArray

                surahs.add(
                    SurahEntity(
                        number = surahNumber,
                        nameArabic = root["name"]!!.jsonPrimitive.content,
                        nameTransliterated = root["transliteration"]!!.jsonPrimitive.content,
                        totalAyahs = versesArray.size
                    )
                )

                versesArray.forEach { entry ->
                    val obj = entry.jsonObject
                    ayahs.add(
                        AyahEntity(
                            surahNumber = surahNumber,
                            numberInSurah = obj["id"]!!.jsonPrimitive.content.toInt(),
                            text = obj["text"]!!.jsonPrimitive.content,
                            normalizedText = obj["normalized"]!!.jsonPrimitive.content
                        )
                    )
                }

                val progress = (index + 1) / 114f
                setProgress(
                    workDataOf(
                        KEY_PROGRESS to progress,
                        KEY_CURRENT_SURAH to surahNumber
                    )
                )
            }

            db.surahDao().insertAll(surahs)
            db.ayahDao().insertAll(ayahs)

            Log.d(TAG, "Population complete: ${surahs.size} surahs, ${ayahs.size} ayahs")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Population failed", e)
            Result.retry()
        }
    }
}