package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hussein.mawaqit.data.db.entities.invocation.AzkarCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.AzkarItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AzkarDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<AzkarCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<AzkarItemEntity>)

    @Query("SELECT * FROM azkar_categories")
    fun getAllCategories(): Flow<List<AzkarCategoryEntity>>

    @Query("SELECT * FROM azkar_items WHERE categoryId = :categoryId")
    fun getItemsForCategory(categoryId: Int): Flow<List<AzkarItemEntity>>

    @Query("SELECT COUNT(*) FROM azkar_categories")
    suspend fun countCategories(): Int
}
