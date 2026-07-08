package com.hussein.mawaqit.data.db.repo

import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.mappers.toAyah
import com.hussein.mawaqit.data.mappers.toTafsir
import com.hussein.mawaqit.domain.models.AyahWithTafsir
import com.hussein.mawaqit.domain.models.Tafsir

class TafsirRepository(val db: AppDatabase) {

    private val tafsirDao = db.tafsirDao()
    private val ayahDao = db.ayahDao()

    suspend fun fetchTafsir(surahNumber: Int, ayahNumber: Int): Tafsir? {
        // TODO:  handle sources
        return tafsirDao.getTafsirForAyah(
            sourceId = "mukhtasar",
            surahNumber = surahNumber,
            ayahNumber = ayahNumber
        )?.toTafsir()
    }

    suspend fun fetchSurahWithTafsir(surahNumber: Int): List<AyahWithTafsir> {
        val ayahs = ayahDao.getAyahsForSurah(surahNumber).map { it.toAyah() }
        val tafsirs = tafsirDao.getMukhtasarTafsirForSurah(surahNumber).associateBy { it.numberInSurah }

        return ayahs.map { ayah ->
            AyahWithTafsir(
                ayah = ayah,
                tafsir = tafsirs[ayah.numberInSurah]?.toTafsir()
            )
        }
    }
}