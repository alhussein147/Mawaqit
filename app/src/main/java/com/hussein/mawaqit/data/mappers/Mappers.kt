package com.hussein.mawaqit.data.mappers

import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.relations.AyahWithSurah
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.AyahOfTheDay

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
