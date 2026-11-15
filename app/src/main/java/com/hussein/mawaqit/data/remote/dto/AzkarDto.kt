package com.hussein.mawaqit.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvocationCategoryDto(
    val id: Int,
    val title: String,
    val content: List<InvocationItemDto>,
    val highlight: Boolean = false,
    val type:String,
    val sortOrder: Int
)

@Serializable
data class InvocationItemDto(
    val text: String,
    val repeat: Int? = null,
    val bless: String? = null
)