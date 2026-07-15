package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hussein.mawaqit.data.db.entities.invocation.InvocationCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.InvocationCategoryType
import com.hussein.mawaqit.data.db.entities.invocation.InvocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvocationDao {

    @Query(
        """
        SELECT * FROM invocation_categories
        ORDER BY sortOrder ASC
        """
    )
    fun getAllCategories(): Flow<List<InvocationCategoryEntity>>


    @Query(
        """
        SELECT * FROM invocation_categories
        WHERE type = :type
        ORDER BY sortOrder ASC
        """
    )
    fun getCategoriesByType(
        type: InvocationCategoryType
    ): Flow<List<InvocationCategoryEntity>>


    @Query(
        """
        SELECT * FROM invocations
        WHERE categoryId = :categoryId
        ORDER BY id ASC
        """
    )
    fun getInvocationsByCategory(
        categoryId: Int
    ): Flow<List<InvocationEntity>>

    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(
        categories: List<InvocationCategoryEntity>
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvocations(
        invocations: List<InvocationEntity>
    )


    @Query("SELECT COUNT(*) FROM invocation_categories")
    suspend fun countCategories(): Int

    @Delete
    suspend fun deleteCategory(
        category: InvocationCategoryEntity
    )
}