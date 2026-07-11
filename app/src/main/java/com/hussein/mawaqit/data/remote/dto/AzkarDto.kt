package com.hussein.mawaqit.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class AzkarCategoryDto(
    val id: Int,
    val title: String,
    val content: List<AzkarItemDto>,
    val highlight: Boolean = false
)

@Serializable
data class AzkarItemDto(
    val zekr: String,
    val repeat: Int? = null,
    val bless: String? = null
)
