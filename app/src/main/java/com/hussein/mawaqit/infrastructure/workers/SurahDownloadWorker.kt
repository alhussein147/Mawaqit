package com.hussein.mawaqit.infrastructure.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.data.RecitationRepository
import com.hussein.mawaqit.data.remote.DownloadService
import kotlinx.coroutines.CancellationException
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

class SurahDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val repo: RecitationRepository by inject()

    // TODO: LIMIT Parallel downloading to only 3 
    companion object {
        const val KEY_SURAH_NUMBER = "surah_number"
        const val KEY_RECITER_ID = "reciter_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_FILE_PATH = "file_path"

        fun inputData(surahNumber: Int, reciterId: Long): Data = workDataOf(
            KEY_SURAH_NUMBER to surahNumber,
            KEY_RECITER_ID to reciterId
        )
    }

    override suspend fun doWork(): Result {
        val surahNumber = inputData.getInt(KEY_SURAH_NUMBER, -1)
        val reciterId = inputData.getLong(KEY_RECITER_ID, -1L)

        if (surahNumber == -1 || reciterId == -1L) return Result.failure()

        val reciter = repo.getAudioSource(reciterId)
            ?: return Result.failure()

        val file = repo.surahFile(reciterId, surahNumber)

        // Already downloaded — nothing to do
        if (file.exists() && file.length() > 0) {
            return Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
        }

        val parentDir = file.parentFile ?: return Result.failure()
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        val tempFile = File(parentDir, "${file.name}.tmp")

        return try {
            val url = repo.surahUrl(reciter, surahNumber) ?: return Result.failure()
            Log.d("DownloadWorkManager", "doWork: $url")
            
            var lastProgress = -1f
            val downloadResult = DownloadService.downloadFile(
                url = url,
                outputFile = tempFile,
                onProgress = { progress ->
                    if (progress - lastProgress >= 0.01f || progress == 1f) {
                        lastProgress = progress
                        try {
                            setProgress(workDataOf(KEY_PROGRESS to progress))
                        } catch (e: Exception) {
                            Log.w("DownloadWorkManager", "Failed to update progress", e)
                        }
                    }
                }
            )

            if (downloadResult.isSuccess) {
                if (file.exists()) file.delete()
                val moved = if (tempFile.renameTo(file)) {
                    true
                } else {
                    try {
                        tempFile.copyTo(file, overwrite = true)
                        tempFile.delete()
                        true
                    } catch (e: Exception) {
                        Log.e("DownloadWorkManager", "Failed to copy temp file to target", e)
                        false
                    }
                }

                if (moved) {
                    Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
                } else {
                    if (tempFile.exists()) tempFile.delete()
                    Result.failure()
                }
            } else {
                Log.e("DownloadWorkManager", "Failed to download", downloadResult.exceptionOrNull())
                if (tempFile.exists()) tempFile.delete()
                Result.failure()
            }
        } catch (e: CancellationException) {
            Log.i("DownloadWorkManager", "Worker cancelled")
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e("DownloadWorkManager", "Error in Worker", e)
            if (tempFile.exists()) tempFile.delete()
            Result.failure()
        }
    }
}