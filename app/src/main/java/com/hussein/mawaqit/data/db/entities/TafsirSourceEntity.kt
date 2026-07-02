package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "tafsir_sources"
)
data class TafsirSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String
)
