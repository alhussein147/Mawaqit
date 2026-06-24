package com.hussein.mawaqit.data.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// TODO:  create a domain model for this entity
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