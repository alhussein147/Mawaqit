package com.hussein.mawaqit.infrastructure.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.data.db.QuranDatabase
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import kotlinx.serialization.json.Json
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TafsirPopulationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val db: QuranDatabase by inject()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            populateTafsir()
//            persist the state of population
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun populateTafsir() {
        val json = applicationContext.assets
            .open("tafsir.json")
            .bufferedReader()
            .use { it.readText() }

        val root = JSONObject(json)

        val total = root.length()

        val batch = mutableListOf<TafsirEntity>()

        var processed = 0

        val keys = root.keys()

        while (keys.hasNext()) {

            val key = keys.next()

            val (surahNumber, ayahNumber) =
                key.split(":").map(String::toInt)

            val text = root
                .getJSONObject(key)
                .getString("text")

            batch += TafsirEntity(
                sourceId = "mukhtasar",
                surahNumber = surahNumber,
                numberInSurah = ayahNumber,
                text = text
            )

            processed++

            if (batch.size >= 500) {
                db.tafsirDao().insertAll(batch)
                batch.clear()
            }

            setProgress(
                workDataOf(
                    PROGRESS to ((processed * 100) / total)
                )
            )
        }

        if (batch.isNotEmpty()) {
            db.tafsirDao().insertAll(batch)
        }

        setProgress(
            workDataOf(PROGRESS to 100)
        )
    }

    companion object {
        const val PROGRESS = "progress"
    }
}