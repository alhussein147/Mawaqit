package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AudioSourceEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val imageUrl: String?,
    val language: String?,

    val surahBaseUrl: String?,
    val ayahBaseUrl: String?,
    val radioStreamUrl: String?
)