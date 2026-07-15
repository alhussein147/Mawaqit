package com.hussein.mawaqit.data.db.entities.invocation

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "invocation_categories"
)
data class InvocationCategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val type: InvocationCategoryType,
    val highlight: Boolean,
    val sortOrder: Int
)
