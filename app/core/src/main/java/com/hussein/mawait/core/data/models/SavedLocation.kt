package com.hussein.mawait.core.data.models

data class SavedLocation(
    val latitude: Double,
    val longitude: Double,
    val cityName: String,
    val savedAt: Long
)