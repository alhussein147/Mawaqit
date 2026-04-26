package com.hussein.core.models

import androidx.compose.runtime.Stable

@Stable
data class SavedLocation(
    val latitude  : Double,
    val longitude : Double,
    val cityName  : String
)