package com.hussein.mawaqit.domain.models

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
@Stable
data class Ayah(
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String
)
