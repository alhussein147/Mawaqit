package com.hussein.mawaqit.data.db.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@Stable
data class Ayah(
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String
)
