package com.hussein.mawaqit.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "surahs")
data class SurahEntity(
    @PrimaryKey val number: Int,
    val nameArabic: String,
    val nameTransliterated: String,
    val totalAyahs: Int
)

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
    val text: String
)

@Entity(
    tableName = "bookmarks",
    foreignKeys = [ForeignKey(
        entity = SurahEntity::class,
        parentColumns = ["number"],
        childColumns = ["surahNumber"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("surahNumber")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val surahNumber: Int,
    val ayahNumber: Int,
    val addedAt: Long = System.currentTimeMillis()
)