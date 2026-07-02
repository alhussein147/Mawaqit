package com.hussein.mawaqit.data.db.relations

data class AyahWithSurah(
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String,
    val surahNameArabic: String,
    val surahTranslate: String
)
