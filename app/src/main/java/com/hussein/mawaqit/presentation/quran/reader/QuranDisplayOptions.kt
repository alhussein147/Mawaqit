package com.hussein.mawaqit.presentation.quran.reader

import kotlinx.serialization.Serializable

enum class QuranFontSize(val sp: Int, val label: String) {
    SMALL(18, "S"),
    MEDIUM(22, "M"),
    LARGE(26, "L"),
    XLARGE(30, "XL")
}

enum class QuranTextAlignment(val displayName: String) {
    Start("Start"), Center("Center"), End("End")
}

@Serializable
data class QuranBookmark(
    val surahIndex: Int,
    val ayahNumber: Int
)
