package com.hussein.mawaqit.data.db.entities.invocation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "invocations",
    foreignKeys = [
        ForeignKey(
            entity = InvocationCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InvocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val content: String,
    val benefit: String?,
    val repeatCount: Int?,
)
