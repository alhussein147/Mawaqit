package com.hussein.mawaqit.infrastructure.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.data.db.AppDatabase.Companion.DB_NAME
import com.hussein.mawaqit.data.remote.DownloadService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DatabasePopulationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val DATABASE_POPULATION_WORK_NAME = "quran_population"
        const val DATABASE_POPULATION_PROGRESS = "progress"
        private const val TAG = "QuranPopulationWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val url = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/db/mawaqit.db"
        val dbFile = applicationContext.getDatabasePath(DB_NAME)
        val tempFile = File(applicationContext.cacheDir, "temp_$DB_NAME")

        Log.d(TAG, "Starting Quran database population from: $url")
        
        var lastProgress = -1f
        DownloadService.downloadFile(url, tempFile) { progress ->
            if (progress - lastProgress >= 0.01f || progress == 1f) {
                lastProgress = progress
                setProgress(workDataOf(DATABASE_POPULATION_PROGRESS to progress))
            }
        }.fold(
            onSuccess = {
                try {
                    Log.d(TAG, "Download successful, replacing database at ${dbFile.absolutePath}")

                    dbFile.parentFile?.mkdirs()
                    tempFile.copyTo(dbFile, overwrite = true)
                    tempFile.delete()

                    Log.d(TAG, "Database population completed successfully")
                    Result.success()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to replace database file", e)
                    Result.failure(
                        workDataOf(
                            "error" to (e.localizedMessage ?: "File replacement failed")
                        )
                    )
                }
            },
            onFailure = { e ->
                Log.e(TAG, "Failed to download Quran database", e)
                Result.failure(workDataOf("error" to (e.localizedMessage ?: "Download failed")))
            }
        )
    }
}
