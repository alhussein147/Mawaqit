package com.hussein.mawaqit.data.remote

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.readAvailable
import java.io.File

object DownloadService {

// refactor
    suspend fun downloadFile(
        url: String,
        outputFile: File,
        onProgress: suspend (Float) -> Unit
    ): Result<Unit> {
        val client = RemoteService.getClient()

        return runCatching {
            val response = client.get(url)

            if (response.status != HttpStatusCode.OK) {
                error("Download failed: ${response.status}")
            }

            val contentLength = response.contentLength() ?: -1L
            val channel = response.bodyAsChannel()
            
            outputFile.outputStream().use { output ->
                val buffer = ByteArray(1024 * 8)
                var bytesRead = 0L
                while (!channel.isClosedForRead) {
                    val read = channel.readAvailable(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    bytesRead += read
                    
                    if (contentLength > 0) {
                        onProgress(bytesRead.toFloat() / contentLength)
                    }
                }
            }
        }.also {
            RemoteService.close()
        }
    }


}
