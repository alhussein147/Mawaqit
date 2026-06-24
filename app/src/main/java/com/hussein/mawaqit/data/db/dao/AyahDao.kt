package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.relations.AyahWithSurah
import kotlinx.coroutines.flow.Flow

@Dao
interface AyahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayahs: List<AyahEntity>)

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah")
    suspend fun getAyahsForSurah(surahNumber: Int): List<AyahEntity>

    @Query(
        """SELECT * FROM ayahs
           WHERE REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(normalizedText, 'ٱ', 'ا'), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ي'), 'ة', 'ه'), 'ـ', '') LIKE '%' || :normalizedQuery || '%'
              OR REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(text, 'ٱ', 'ا'), 'أ', 'ا'), 'إ', 'ا'), 'آ', 'ا'), 'ى', 'ي'), 'ؤ', 'و'), 'ئ', 'ي'), 'ة', 'ه'), 'ـ', '') LIKE '%' || :normalizedQuery || '%'
              OR text LIKE '%' || :rawQuery || '%'
           ORDER BY surahNumber, numberInSurah"""
    )
    fun searchAyahs(rawQuery: String, normalizedQuery: String): Flow<List<AyahEntity>>

    // Ayah of the day joined with its surah in a single query
    @Query(
        """SELECT ayahs.surahNumber, ayahs.numberInSurah, ayahs.text,
              surahs.nameArabic AS surahNameArabic, surahs.nameTransliterated AS surahTranslit
       FROM ayahs
       JOIN surahs ON ayahs.surahNumber = surahs.number
       LIMIT 1
       OFFSET (ABS((:seed * 1664525 + 1013904223) & 2147483647) % (SELECT COUNT(*) FROM ayahs))"""
    )
    suspend fun getAyahOfTheDay(seed: Long): AyahWithSurah?
    // Deterministic random for today using date as seed

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun count(): Int
}
