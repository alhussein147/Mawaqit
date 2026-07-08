package com.hussein.mawaqit.data.remote.models


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ZikrCategoryDto(
    @SerialName("content")
    val zikrContent: List<ZikrContent>,
    @SerialName("highlight")
    val highlight: Boolean,
    @SerialName("id")
    val id: Int,
    @SerialName("title")
    val title: String
)