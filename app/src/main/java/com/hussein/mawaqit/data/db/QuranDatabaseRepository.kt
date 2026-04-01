package com.hussein.mawaqit.data.db

import com.hussein.mawaqit.data.db.models.Ayah
import com.hussein.mawaqit.data.db.models.AyahOfTheDay
import com.hussein.mawaqit.data.db.models.AyahWithSurah
import com.hussein.mawaqit.data.db.models.SurahDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
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
    }    // ── Search ────────────────────────────────────────────────────────────────

    fun searchAyahs(query: String): Flow<List<AyahEntity>> =
        ayahDao.searchAyahs(query)

    fun searchSurahs(query: String) =
        surahDao.searchSurahs(query)

    // ── Ayah of the day ───────────────────────────────────────────────────────

    @OptIn(ExperimentalTime::class)
    suspend fun getAyahOfTheDay(): AyahOfTheDay? {
        val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
        val seed = (today.year * 10000L + today.monthNumber * 100 + today.dayOfMonth)
        return ayahDao.getAyahOfTheDay(seed)?.toAyahOfTheDay()
    }

    // ── Bookmarks — Room-backed, supports multiple ────────────────────────────

    fun getAllBookmarks(): Flow<List<BookmarkEntity>> =
        bookmarkDao.getAllBookmarks()

    fun isBookmarked(surahNumber: Int, ayahNumber: Int): Flow<Boolean> =
        bookmarkDao.isBookmarked(surahNumber, ayahNumber)

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun AyahEntity.toAyah() = Ayah(
        surahNumber = surahNumber,
        numberInSurah = numberInSurah,
        text = text
    )
    private fun AyahWithSurah.toAyahOfTheDay(): AyahOfTheDay {
        return AyahOfTheDay(
            numberInSurah = this.numberInSurah,
            text = this.text,
            surahNameArabic = this.surahNameArabic,
            surahTranslit = this.surahTranslit
        )
    }

}

