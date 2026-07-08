package com.hussein.mawaqit.data.mappers

import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.BookmarkEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.relations.AyahWithSurah
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.AyahOfTheDay
import com.hussein.mawaqit.domain.models.Bookmark
import com.hussein.mawaqit.domain.models.Surah
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
        surahTranslit = this.surahTranslate,
        surahIndex = this.surahNumber
    )
}

fun TafsirEntity.toTafsir(): Tafsir = Tafsir(
    surahNumber = this.surahNumber,
    ayahNumber = this.numberInSurah,
    text = this.text
)

fun SurahEntity.toSurah() = Surah(
    number = this.number,
    nameArabic = this.nameArabic,
    nameTransliterated = this.transliteration,
    numberOfAyahs = this.totalAyahs,
    origin = this.origin
)

fun BookmarkEntity.toBookmark(): Bookmark{
    return Bookmark(
        surahNumber = this.surahNumber,
        ayahNumber = this.ayahNumber,
        nameArabic = this.nameArabic,
        addedAt = this.addedAt // handle formatting
    )
}
