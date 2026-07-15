package com.hussein.mawaqit.infrastructure.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hussein.mawaqit.data.remote.RemoteService
import com.hussein.mawaqit.infrastructure.fonts.DynamicFontManager
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.io.FileOutputStream

class FontDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), KoinComponent {

    private val dynamicFontManager: DynamicFontManager by inject()

    override suspend fun doWork(): Result {
        val fontsDir = File(applicationContext.filesDir, "fonts")
        if (!fontsDir.exists()) {
            fontsDir.mkdirs()
        }

        val fonts = listOf(
            FontInfo("regular.ttf", REGULAR_FONT_URL),
            FontInfo("bold.ttf", BOLD_FONT_URL)
        )

        return try {
            val client = RemoteService.getClient()
            
            withContext(Dispatchers.IO) {
                for (font in fonts) {
                    val response = client.get(font.url)
                    if (response.status == HttpStatusCode.OK) {
                        val bytes = response.bodyAsBytes()
                        val file = File(fontsDir, font.name)
                        FileOutputStream(file).use { it.write(bytes) }
                    } else {
                        throw Exception("Failed to download font: ${font.name}")
                    }
                }
            }
            
            // Reload fonts in manager after successful download
            dynamicFontManager.loadFonts()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private data class FontInfo(val name: String, val url: String)

    companion object {
        // REPLACE THESE WITH YOUR ACTUAL CDN URLs
        private const val REGULAR_FONT_URL = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/fonts/regular.ttf"
        private const val BOLD_FONT_URL = "https://dvtajbmeveppcffgfnog.supabase.co/storage/v1/object/public/assets/fonts/bold.ttf"
    }
}
