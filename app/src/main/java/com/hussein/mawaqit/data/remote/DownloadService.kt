package com.hussein.mawaqit.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File

object DownloadService {

    const val SUPABASE_MAWAQIT_DB =
        "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/db/mawaqit.db"


    suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> {
        val client = HttpClient()
        return runCatching {
            val response = client.get(url) {
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        onProgress(bytesSentTotal.toFloat() / contentLength)
                    }
                }
            }

            if (response.status != HttpStatusCode.OK) {
                error("Download failed: ${response.status}")
            }

            // Stream the response body to the file to save memory
            val channel = response.bodyAsChannel()
            outputFile.outputStream().use { output ->
                channel.copyTo(output)
            }
            Unit
        }.also {
            client.close()
        }
    }
}
