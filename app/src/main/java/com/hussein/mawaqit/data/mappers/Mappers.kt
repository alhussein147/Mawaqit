package com.hussein.mawaqit.data.mappers

import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.relations.AyahWithSurah
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.AyahOfTheDay
import com.hussein.mawaqit.domain.models.Tafsir

fun AyahEntity.toAyah() = Ayah(
    surahNumber = surahNumber,
    numberInSurah = numberInSurah,
    text = text
)

fun AyahWithSurah.toAyahOfTheDay(): AyahOfTheDay {
    return AyahOfTheDay(
        numberInSurah = this.numberInSurah,
        text = this.text,
        surahNameArabic = this.surahNameArabic,
        surahTranslit = this.surahTranslit,
        surahIndex = this.surahNumber
    )
}

fun TafsirEntity.toTafsir(): Tafsir = Tafsir(
    surahNumber = this.surahNumber,
    ayahNumber = this.numberInSurah,
    text = this.text
)
