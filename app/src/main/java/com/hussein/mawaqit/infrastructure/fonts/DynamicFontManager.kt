package com.hussein.mawaqit.infrastructure.fonts

import android.content.Context
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hussein.mawaqit.infrastructure.workers.FontDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class DynamicFontManager(
    private val context: Context,
    private val workManager: WorkManager
) {
    private val fontsDir = File(context.filesDir, "fonts")
    private val regularFontFile = File(fontsDir, "regular.ttf")
    private val boldFontFile = File(fontsDir, "bold.ttf")

    private val _fontFamily = MutableStateFlow<FontFamily>(FontFamily.Default)
    val fontFamily: StateFlow<FontFamily> = _fontFamily.asStateFlow()

    init {
        loadFonts()
    }

    fun loadFonts() {
        if (regularFontFile.exists() && boldFontFile.exists()) {
            _fontFamily.value = FontFamily(
                Font(regularFontFile, FontWeight.Normal),
                Font(boldFontFile, FontWeight.Bold)
            )
        } else {
            triggerDownload()
        }
    }

    private fun triggerDownload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadRequest = OneTimeWorkRequestBuilder<FontDownloadWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueue(downloadRequest)
    }
}
