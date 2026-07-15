package com.hussein.mawaqit.data

import android.content.Context
import com.hussein.mawaqit.data.db.dao.AudioSourceDao
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecitationRepository(
    private val context: Context,
    private val audioSourceDao: AudioSourceDao
) {

    suspend fun getAudioSource(id: Long): AudioSourceEntity? = audioSourceDao.getById(id)

    fun getAyahReciters(): Flow<List<AudioSourceEntity>> = audioSourceDao.getAyahReciters()

    fun getSurahReciters(): Flow<List<AudioSourceEntity>> = audioSourceDao.getSurahReciters()

    fun getRadioSources(): Flow<List<AudioSourceEntity>> = audioSourceDao.getRadioSources()

    fun ayahUrl(reciter: AudioSourceEntity, surahNumber: Int, ayahNumber: Int): String? {
        val baseUrl = reciter.ayahBaseUrl ?: return null
        return "$baseUrl/${surahNumber}_${ayahNumber}.mp3"
    }

    fun surahUrl(reciter: AudioSourceEntity, surahNumber: Int): String? {
        val baseUrl = reciter.surahBaseUrl ?: return null
        return "$baseUrl/${surahNumber.toString().padStart(3, '0')}.mp3"
    }

    fun surahFile(reciterId: Long, surahNumber: Int): File {
        val dir = File(context.filesDir, "recitation/$reciterId")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "$surahNumber.mp3")
    }

    fun isSurahCached(reciterId: Long, surahNumber: Int): Boolean =
        surahFile(reciterId, surahNumber).exists()
}
