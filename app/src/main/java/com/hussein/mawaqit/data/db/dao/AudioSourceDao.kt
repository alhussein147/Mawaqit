package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioSourceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sources: List<AudioSourceEntity>)

    @Query("SELECT * FROM AudioSourceEntity")
    fun getAll(): Flow<List<AudioSourceEntity>>

    @Query("SELECT * FROM AudioSourceEntity WHERE radioStreamUrl IS NOT NULL")
    fun getRadioSources(): Flow<List<AudioSourceEntity>>

    @Query("SELECT * FROM AudioSourceEntity WHERE surahBaseUrl IS NOT NULL")
    fun getSurahReciters(): Flow<List<AudioSourceEntity>>

    @Query("SELECT * FROM AudioSourceEntity WHERE ayahBaseUrl IS NOT NULL")
    fun getAyahReciters(): Flow<List<AudioSourceEntity>>

    @Query("SELECT * FROM AudioSourceEntity WHERE id = :id")
    suspend fun getById(id: Long): AudioSourceEntity?

    @Query("SELECT COUNT(*) FROM AudioSourceEntity")
    suspend fun count(): Int

    @Query("DELETE FROM AudioSourceEntity")
    suspend fun deleteAll()
}
