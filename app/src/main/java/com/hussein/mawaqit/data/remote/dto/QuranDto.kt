package com.hussein.mawaqit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranSurahDto(
    @SerialName("id")
    val id: Int,
    @SerialName("name")
    val name: String,
    @SerialName("transliteration")
    val transliteration: String,
    @SerialName("type")
    val type: String,
    @SerialName("verses")
    val verses: List<QuranAyahDto>
)

@Serializable
data class QuranAyahDto(
    @SerialName("id")
    val id: Int,
    @SerialName("text")
    val text: String,
    @SerialName("normalized")
    val normalized: String?
)
