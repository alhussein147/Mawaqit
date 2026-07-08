package com.hussein.mawaqit.infrastructure.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.domain.models.FullSurahReciter
import com.hussein.mawaqit.data.quran.recitation.SurahDownloadRepository
import com.hussein.mawaqit.data.remote.DownloadService
import java.io.File

class SurahDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // TODO: LIMIT Parallel downloading to only 3 
    companion object {
        const val KEY_SURAH_NUMBER = "surah_number"
        const val KEY_RECITER_ID = "reciter_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"

        fun inputData(surahNumber: Int, reciterId: Int): Data = workDataOf(
            KEY_SURAH_NUMBER to surahNumber,
            KEY_RECITER_ID to reciterId
        )
    }

    override suspend fun doWork(): Result {
        val surahNumber = inputData.getInt(KEY_SURAH_NUMBER, -1)
        val reciterId = inputData.getInt(KEY_RECITER_ID, -1)

        if (surahNumber == -1 || reciterId == -1) return Result.failure()

        val reciter = FullSurahReciter.entries.find { it.id == reciterId }
            ?: return Result.failure()

        val repo = SurahDownloadRepository(applicationContext)
        val file = repo.surahFile(reciter, surahNumber)

        // Already downloaded — nothing to do
        if (file.exists()) {
            return Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
        }

        return try {
            val url = repo.surahUrl(reciter, surahNumber)
            Log.d("DownloadWorkManager", "doWork:$url ")
            
            val tempFile = File(file.parent, "${file.name}.tmp")
            var lastProgress = -1f
            val downloadResult = DownloadService.downloadFile(
                url = url,
                outputFile = tempFile,
                onProgress = { progress ->
                    if (progress - lastProgress >= 0.01f || progress == 1f) {
                        lastProgress = progress
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                }
            )

            if (downloadResult.isSuccess) {
                if (tempFile.renameTo(file)) {
                    Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
                } else {
                    tempFile.delete()
                    Result.failure()
                }
            } else {
                Log.e("DownloadWorkManager", "Failed to download", downloadResult.exceptionOrNull())
                tempFile.delete()
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("DownloadWorkManager", "Error in Worker", e)
            file.delete()
            Result.failure()
        }
    }
}