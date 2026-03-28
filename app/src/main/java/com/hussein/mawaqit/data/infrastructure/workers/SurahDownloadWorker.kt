package com.hussein.mawaqit.data.infrastructure.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.hussein.mawaqit.data.quran.recitation.FullSurahReciter
import com.hussein.mawaqit.data.quran.recitation.RecitationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

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

        val repo = RecitationRepository(applicationContext)
        val file = repo.surahFile(reciter, surahNumber)

        // Already downloaded — nothing to do
        if (file.exists()) {
            return Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
        }

        return try {
            val url = repo.surahUrl(reciter, surahNumber)
            Log.d("DownloadWorkManager", "doWork:$url ")
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connect()
                
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("DownloadWorkManager", "Failed to download: ${connection.responseCode}")
                    return@withContext Result.failure()
                }

                val totalBytes = connection.contentLength.toFloat()
                val tempFile = File(file.parent, "${file.name}.tmp")
                connection.getInputStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead = 0L
                        var read: Int

                        while (input.read(buffer).also { read = it } != -1) {
                            // Canceled by user — clean up and exit
                            if (isStopped) {
                                tempFile.delete()
                                return@withContext Result.failure()
                            }
                            output.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                val progress = (bytesRead / totalBytes).coerceIn(0f, 1f)
                                setProgress(workDataOf(KEY_PROGRESS to progress))
                            }
                        }
                    }
                }

                // Rename temp file to final only on full success
                if (tempFile.renameTo(file)) {
                    Result.success(workDataOf(KEY_FILE_PATH to file.absolutePath))
                } else {
                    tempFile.delete()
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadWorkManager", "Error downloading", e)
            file.delete()
            Result.failure()
        }
    }
}