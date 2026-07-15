package com.hussein.mawaqit.data.db.entities.invocation

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "azkar_categories")
data class AzkarCategoryEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val highlight: Boolean
)
