package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity

@Dao
interface TafsirDao {

    @Insert
    suspend fun insertSource(tafsirSource: TafsirSourceEntity)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        tafsir: List<TafsirEntity>
    )

    @Query(
        """
        SELECT *
        FROM tafsir
        WHERE sourceId = :sourceId
          AND surahNumber = :surahNumber
          AND numberInSurah = :ayahNumber
    """
    )
    suspend fun getTafsirForAyah(
        sourceId: String,
        surahNumber: Int,
        ayahNumber: Int
    ): TafsirEntity?

    @Query(
        """
        SELECT *
        FROM tafsir
        WHERE sourceId = :sourceId
          AND surahNumber = :surahNumber
        ORDER BY numberInSurah ASC
    """
    )
    suspend fun getTafsirForSurah(
        sourceId: String,
        surahNumber: Int
    ): List<TafsirEntity>

    @Query("SELECT COUNT(*) FROM tafsir")
    suspend fun count(): Int

    @Query(
        """
    SELECT *
    FROM tafsir
    WHERE sourceId = 'mukhtasar'
      AND surahNumber = :surahNumber
      AND numberInSurah = :ayahNumber
"""
    )
    suspend fun getMukhtasarTafsirForAyah(
        surahNumber: Int,
        ayahNumber: Int
    ): TafsirEntity?

    @Query(
        """
    SELECT *
    FROM tafsir
    WHERE sourceId = 'mukhtasar'
      AND surahNumber = :surahNumber
    ORDER BY numberInSurah
"""
    )
    suspend fun getMukhtasarTafsirForSurah(
        surahNumber: Int
    ): List<TafsirEntity>
}


