package com.hussein.core.models

import androidx.compose.runtime.Stable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class UserLocation(
    @SerialName("lat")
    val latitude  : Double,
    @SerialName("lon")
    val longitude : Double,
    val altitude: Double? = null,
    val cityName  : String
)