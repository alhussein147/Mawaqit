package com.hussein.mawaqit.data.quran.tafsir.models

import kotlinx.serialization.Serializable

@Serializable
data class TafsirResponse(
    val code: Int,
    val status: String,
    val data: TafsirData
)

@Serializable
data class TafsirData(
    val number: Int,
    val text: String,
    val numberInSurah: Int
)