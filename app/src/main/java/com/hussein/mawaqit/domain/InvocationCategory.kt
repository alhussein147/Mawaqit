package com.hussein.mawaqit.domain

data class InvocationCategory(
    val id: Int,
    val title: String,
    val highlight: Boolean = false,
    val type: String,
    val sortOrder: Int
)

data class InvocationItem(
    val text: String,
    val repeat: Int? = null,
    val bless: String? = null
)