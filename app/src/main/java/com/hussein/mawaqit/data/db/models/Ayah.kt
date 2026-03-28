package com.hussein.mawaqit.data.db.models

import androidx.compose.runtime.Immutable

@Immutable
data class Ayah(
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String
)
