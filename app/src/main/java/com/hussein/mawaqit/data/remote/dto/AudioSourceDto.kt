package com.hussein.mawaqit.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AudioSourceDto(
    @SerialName("id")
    val id: Long,
    @SerialName("nameEn")
    val nameEn: String?,
    @SerialName("nameAr")
    val nameAr: String?,
    @SerialName("imageUrl")
    val imageUrl: String?,
    @SerialName("language")
    val language: String?,
    @SerialName("sources")
    val sources: AudioSourcesDto?
)

@Serializable
data class AudioSourcesDto(
    @SerialName("ayah")
    val ayah: AudioSourceUrlDto? = null,
    @SerialName("surah")
    val surah: AudioSourceUrlDto? = null,
    @SerialName("radio")
    val radio: AudioSourceUrlDto? = null
)

@Serializable
data class AudioSourceUrlDto(
    @SerialName("baseUrl")
    val baseUrl: String? = null,
    @SerialName("url")
    val url: String? = null
)
