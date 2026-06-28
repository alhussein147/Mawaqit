package com.hussein.mawaqit.data.quran.tafsir

import com.hussein.mawaqit.data.db.QuranDatabase
import com.hussein.mawaqit.data.mappers.toTafsir
import com.hussein.mawaqit.domain.models.Tafsir

class TafsirRepository(val db: QuranDatabase) {

    val dao = db.tafsirDao()

    suspend fun fetchTafsir(surahNumber: Int, ayahNumber: Int): Tafsir? {
        // TODO:  handle sources
        return dao.getTafsirForAyah(
            sourceId = "mukhtasar",
            surahNumber = surahNumber,
            ayahNumber = ayahNumber
        )?.toTafsir()
    }
}
