package com.hussein.mawaqit.data.db.repo

import com.hussein.mawaqit.data.db.dao.AyahDao
import com.hussein.mawaqit.data.db.dao.BookmarkDao
import com.hussein.mawaqit.data.db.dao.SurahDao
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.BookmarkEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.mappers.toAyah
import com.hussein.mawaqit.data.mappers.toAyahOfTheDay
import com.hussein.mawaqit.data.quran.ArabicSearchNormalizer
import com.hussein.mawaqit.domain.models.AyahOfTheDay
import com.hussein.mawaqit.domain.models.SurahDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class QuranDatabaseRepository(
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
    private val bookmarkDao: BookmarkDao
) {

    fun getAllSurahs(): Flow<List<SurahEntity>> = surahDao.getAllSurahs()


    suspend fun loadSurah(surahNumber: Int): SurahDetail {
        val result = surahDao.getSurahWithAyahs(surahNumber)
            ?: return SurahDetail(surahNumber, "", "", emptyList())
        return SurahDetail(
            number = result.surah.number,
            nameArabic = result.surah.nameArabic,
            nameTransliterated = result.surah.nameTransliterated,
            ayahs = result.ayahs.map { it.toAyah() }
        )
    }

    fun searchAyahs(query: String): Flow<List<AyahEntity>> =
        ayahDao.searchAyahs(
            rawQuery = query.trim(),
            normalizedQuery = ArabicSearchNormalizer.normalize(query)
        )

    fun searchSurahs(query: String) =
        surahDao.searchSurahs(
            rawQuery = query.trim(),
            normalizedQuery = ArabicSearchNormalizer.normalize(query)
        )


    @OptIn(ExperimentalTime::class)
    suspend fun getAyahOfTheDay(): AyahOfTheDay? {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val seed = (today.year * 10000L + today.month.number * 100 + today.day)
        return ayahDao.getAyahOfTheDay(seed)?.toAyahOfTheDay()
    }


    fun getAllBookmarks(): Flow<List<BookmarkEntity>> =
        bookmarkDao.getAllBookmarks()


    suspend fun addBookmark(surahNumber: Int, ayahNumber: Int) {
        bookmarkDao.insert(BookmarkEntity(surahNumber = surahNumber, ayahNumber = ayahNumber))
    }

    suspend fun removeBookmark(surahNumber: Int, ayahNumber: Int) {
        bookmarkDao.delete(surahNumber, ayahNumber)
    }

    suspend fun toggleBookmark(surahNumber: Int, ayahNumber: Int) {
        val exists = bookmarkDao.countForAyah(surahNumber, ayahNumber) > 0
        if (exists) removeBookmark(surahNumber, ayahNumber)
        else addBookmark(surahNumber, ayahNumber)
    }



}