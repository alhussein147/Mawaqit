package com.hussein.mawaqit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TafsirDto(
    @SerialName("surah")
    val surah: Int,
    @SerialName("ayahNumber")
    val ayahNumber: Int,
    @SerialName("text")
    val text: String
)
