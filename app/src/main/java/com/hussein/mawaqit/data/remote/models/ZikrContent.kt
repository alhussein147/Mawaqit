package com.hussein.mawaqit.data.remote.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZikrContent(
    @SerialName("bless")
    val bless: String,
    @SerialName("repeat")
    val repeat: Int,
    @SerialName("zekr")
    val zekr: String
)