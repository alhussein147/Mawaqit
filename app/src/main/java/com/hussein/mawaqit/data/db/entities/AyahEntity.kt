package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "ayahs",
    primaryKeys = ["surahNumber", "numberInSurah"],
    foreignKeys = [ForeignKey(
        entity = SurahEntity::class,
        parentColumns = ["number"],
        childColumns = ["surahNumber"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("surahNumber")]
)
data class AyahEntity(
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String,
    val normalizedText: String
)