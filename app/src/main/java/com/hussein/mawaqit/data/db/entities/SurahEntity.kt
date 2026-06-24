package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val totalAyahs: Int
)
