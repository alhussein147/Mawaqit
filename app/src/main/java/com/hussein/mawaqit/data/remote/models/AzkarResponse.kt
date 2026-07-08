package com.hussein.mawaqit.data.remote.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AzkarResponse(
    @SerialName("azkar")
    val azkar: List<ZikrCategoryDto>,
    @SerialName("metadata")
    val metadata: Metadata
)