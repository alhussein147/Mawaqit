package com.hussein.mawaqit.domain.models

data class SurahDetail(
    val number: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val ayahs: List<Ayah>
)