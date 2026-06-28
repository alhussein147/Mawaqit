package com.hussein.mawaqit.infrastructure.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.data.db.QuranDatabase
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
data class AyahTafsir(val text: String)

class TafsirPopulationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params), KoinComponent {

    private val db: QuranDatabase by inject()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            populateTafsir()
            Log.d("TafsirPopulationWorker", "Population complete")
            Result.success()
        } catch (e: Exception) {
            Result.failure(workDataOf("error" to e.message))
        }
    }

    private suspend fun populateTafsir() {

        val raw = applicationContext.assets
            .open("tafsir/tafsir.json")
            .bufferedReader()
            .use { it.readText() }

        db.tafsirDao().insertSource(
            TafsirSourceEntity(
                id = "mukhtasar",
                name = "Tafsir Al-Mukhtasar"
            )
        )


        val root = json.parseToJsonElement(raw).jsonArray

        val total = root.size
        val batch = mutableListOf<TafsirEntity>()

        var processed = 0

        root.forEach {
            val surahNumber = it.jsonObject["surah"]!!.jsonPrimitive.content.toInt()
            val ayahNumber = it.jsonObject["ayahNumber"]!!.jsonPrimitive.content.toInt()
            val text = it.jsonObject["text"]!!.jsonPrimitive.content

            batch += TafsirEntity(
                sourceId = "mukhtasar",
                surahNumber = surahNumber,
                numberInSurah =ayahNumber ,
                text = text
            )
            processed++

            if (batch.size >= 500) {
                db.tafsirDao().insertAll(batch)
                batch.clear()
            }

            setProgress(
                workDataOf(
                    KEY_PROGRESS to (processed * 100 / total)
                )
            )

        }


        if (batch.isNotEmpty()) {
            db.tafsirDao().insertAll(batch)
        }

        setProgress(workDataOf(KEY_PROGRESS to 100))
    }

    companion object {
        const val TAFSIR_POPULATION_WORK_NAME = "tafsir_population"
        const val KEY_PROGRESS = "progress"
    }
}