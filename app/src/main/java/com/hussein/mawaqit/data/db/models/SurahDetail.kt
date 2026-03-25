package com.hussein.mawaqit.data.db.models

data class SurahDetail(
    val number: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val ayahs: List<Ayah>
)