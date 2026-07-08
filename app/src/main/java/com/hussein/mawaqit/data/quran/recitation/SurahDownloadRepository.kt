package com.hussein.mawaqit.data.quran.recitation

import android.content.Context
import com.hussein.mawaqit.domain.models.FullSurahReciter
import com.hussein.mawaqit.domain.models.Reciter
import java.io.File

class SurahDownloadRepository(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://the-quran-project.github.io/Quran-Audio/Data"

    }

    fun ayahUrl(reciter: Reciter, surahNumber: Int, ayahNumber: Int): String =
        "$BASE_URL/${reciter.id}/${surahNumber}_${ayahNumber}.mp3"

    fun surahUrl(reciter: FullSurahReciter, surahNumber: Int): String =
        "${reciter.url}/${surahNumber.toString().padStart(3, '0')}.mp3"

    fun surahFile(reciter: FullSurahReciter, surahNumber: Int): File {
        val dir = File(context.filesDir, "recitation/${reciter.id}")
        dir.mkdirs()
        return File(dir, "$surahNumber.mp3")
    }

    fun isSurahCached(reciter: FullSurahReciter, surahNumber: Int): Boolean =
        surahFile(reciter, surahNumber).exists()

}