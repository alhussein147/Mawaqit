package com.hussein.mawaqit.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TafsirDao {

    @Upsert
    suspend fun upsertSource(tafsirSource: TafsirSourceEntity)

    @Upsert
    suspend fun upsertSources(sources: List<TafsirSourceEntity>)

    @Query("UPDATE tafsir_sources SET name = :name, nameAr = :nameAr, lang = :lang, url = :url WHERE id = :id")
    suspend fun updateSourceMetadata(id: String, name: String, nameAr: String, lang: String, url: String)


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

    @Query("DELETE FROM tafsir WHERE sourceId = :sourceId")
    suspend fun deleteTafsirBySource(sourceId: String)

    @Query("SELECT COUNT(*) FROM tafsir WHERE sourceId = :sourceId")
    suspend fun countBySource(sourceId: String): Int

    @Query("UPDATE tafsir_sources SET downloaded = :downloaded WHERE id = :sourceId")
    suspend fun updateSourceDownloaded(sourceId: String, downloaded: Boolean)

    @Query("UPDATE tafsir_sources SET isActive = (id = :sourceId)")
    suspend fun setActiveSource(sourceId: String)

    @Query("SELECT id FROM tafsir_sources WHERE isActive = 1 LIMIT 1")
    fun getActiveSourceId(): Flow<String?>

    @Query("SELECT * FROM tafsir_sources WHERE id = :sourceId")
    suspend fun getSourceById(sourceId: String): TafsirSourceEntity?

    @Query("SELECT * FROM tafsir_sources")
    fun getAllSources(): Flow<List<TafsirSourceEntity>>

    @Query("SELECT * FROM tafsir_sources")
    suspend fun getAllSourcesList(): List<TafsirSourceEntity>
}


