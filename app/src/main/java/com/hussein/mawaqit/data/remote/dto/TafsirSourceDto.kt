package com.hussein.mawaqit.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TafsirSourceDto(
    val id: String,
    val name: String,
    val nameAr: String,
    val language: String,
    val url: String
)
