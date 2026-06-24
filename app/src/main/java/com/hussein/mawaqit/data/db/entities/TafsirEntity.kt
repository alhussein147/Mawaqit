package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tafsir_sources"
)
data class TafsirSourceEntity(
    @PrimaryKey
    val id: String,
    val name: String
)

@Entity(
    tableName = "tafsir",
    primaryKeys = [
        "sourceId",
        "surahNumber",
        "numberInSurah"
    ],
    foreignKeys = [
        ForeignKey(
            entity = AyahEntity::class,
            parentColumns = [
                "surahNumber",
                "numberInSurah"
            ],
            childColumns = [
                "surahNumber",
                "numberInSurah"
            ],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("surahNumber", "numberInSurah")
    ]
)

data class TafsirEntity(
    val sourceId: String,
    val surahNumber: Int,
    val numberInSurah: Int,
    val text: String
)