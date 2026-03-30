package com.hussein.mawaqit.data.db.models

data class AyahOfTheDay(
    val numberInSurah: Int,
    val text: String,
    val surahNameArabic: String,
    val surahTranslit: String
)