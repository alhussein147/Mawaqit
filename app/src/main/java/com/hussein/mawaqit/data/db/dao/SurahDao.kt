package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.db.relations.SurahWithAyahs
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahEntity>)

    @Query("SELECT * FROM surahs ORDER BY number ASC")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :number LIMIT 1")
    suspend fun getSurah(number: Int): SurahEntity?

    @Transaction
    @Query("SELECT * FROM surahs WHERE number = :number LIMIT 1")
    suspend fun getSurahWithAyahs(number: Int): SurahWithAyahs?

    @Query(
        """SELECT * FROM surahs
           WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(nameArabic, 'ٱ', 'ا'), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ي'), 'ة', 'ه'), 'ـ', '') LIKE '%' || :normalizedQuery || '%'
              OR LOWER(transliteration) LIKE '%' || :normalizedQuery || '%'
              OR nameArabic LIKE '%' || :rawQuery || '%'
           ORDER BY number"""
    )
    fun searchSurahs(rawQuery: String, normalizedQuery: String): Flow<List<SurahEntity>>

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun count(): Int
}