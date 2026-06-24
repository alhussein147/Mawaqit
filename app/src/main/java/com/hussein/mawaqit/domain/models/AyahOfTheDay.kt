package com.hussein.mawaqit.domain.models

data class AyahOfTheDay(
    val numberInSurah: Int,
    val text: String,
    val surahNameArabic: String,
    val surahTranslit: String,
    val surahIndex: Int
)