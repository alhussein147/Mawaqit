package com.hussein.mawaqit.data.db.entities.invocation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "azkar_items",
    foreignKeys = [
        ForeignKey(
            entity = AzkarCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AzkarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val zekr: String,
    val repeat: Int?,
    val bless: String?
)
