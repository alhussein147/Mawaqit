package com.hussein.mawaqit.domain.models

data class Bookmark(
    val surahNumber: Int, // needed for navigation
    val ayahNumber: Int,
    val nameArabic: String,
    val addedAt: Long = System.currentTimeMillis() // todo format this
)
