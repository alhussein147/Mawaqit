package com.hussein.mawaqit.data.remote.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Metadata(
    @SerialName("updatedAt")
    val updatedAt: String,
    @SerialName("version")
    val version: Int
)