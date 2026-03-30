package com.hussein.mawaqit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.hussein.mawaqit.data.db.models.AyahWithSurah
import com.hussein.mawaqit.data.db.models.SurahWithAyahs
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahEntity>)

    @Query("SELECT * FROM surahs ORDER BY number")
    fun getAllSurahs(): Flow<List<SurahEntity>>

    @Query("SELECT * FROM surahs WHERE number = :number LIMIT 1")
    suspend fun getSurah(number: Int): SurahEntity?

    @Transaction
    @Query("SELECT * FROM surahs WHERE number = :number LIMIT 1")
    suspend fun getSurahWithAyahs(number: Int): SurahWithAyahs?

    @Query("SELECT * FROM surahs WHERE nameArabic LIKE '%' || :query || '%' OR nameTransliterated LIKE '%' || :query || '%'")
    fun searchSurahs(query: String): Flow<List<SurahEntity>>

    @Query("SELECT COUNT(*) FROM surahs")
    suspend fun count(): Int
}


//@Dao
//interface QuranFtsDao {
//    @Query("SELECT * FROM ayah_fts WHERE ayah_fts MATCH :query")
//    fun searchAyahs(query: String): Flow<List<AyahEntity>>
//}


@Dao
interface AyahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayahs: List<AyahEntity>)

    @Query("SELECT * FROM ayahs WHERE surahNumber = :surahNumber ORDER BY numberInSurah")
    suspend fun getAyahsForSurah(surahNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayahs WHERE normalizedText LIKE '%' || :query || '%' OR text LIKE '%' || :query || '%'")
    fun searchAyahs(query: String): Flow<List<AyahEntity>>

    // Ayah of the day joined with its surah in a single query
    @Query("""SELECT ayahs.surahNumber, ayahs.numberInSurah, ayahs.text,
              surahs.nameArabic AS surahNameArabic, surahs.nameTransliterated AS surahTranslit
       FROM ayahs
       JOIN surahs ON ayahs.surahNumber = surahs.number
       ORDER BY ((ayahs.surahNumber * 1000 + ayahs.numberInSurah) * :seed) % (SELECT COUNT(*) FROM ayahs)
       LIMIT 1""")
    suspend fun getAyahOfTheDay(seed: Long): AyahWithSurah?
    // Deterministic random for today using date as seed

    @Query("SELECT COUNT(*) FROM ayahs")
    suspend fun count(): Int
}

@Dao
interface BookmarkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun delete(surahNumber: Int, ayahNumber: Int)

    @Query("SELECT * FROM bookmarks ORDER BY addedAt DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber)")
    fun isBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean>

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun countForAyah(surahNumber: Int, ayahNumber: Int): Int
}