package com.hussein.mawaqit.data.db

import android.content.Context
import com.hussein.mawaqit.data.db.models.Ayah
import com.hussein.mawaqit.data.db.models.SurahDetail
import com.hussein.mawaqit.data.quran.QuranData
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.ExperimentalTime

class QuranDatabaseRepository(
    private val context: Context,
    private val surahDao: SurahDao,
    private val ayahDao: AyahDao,
    private val bookmarkDao: BookmarkDao
) {

    // ── Surah loading ─────────────────────────────────────────────────────────

    private val cache = object : LinkedHashMap<Int, SurahDetail>(3, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Int, SurahDetail>) = size > 2
    }

    suspend fun loadSurah(surahNumber: Int): SurahDetail {
        cache[surahNumber]?.let { return it }

        val surahEntity = surahDao.getSurah(surahNumber)
        val ayahEntities = ayahDao.getAyahsForSurah(surahNumber)

        return SurahDetail(
            number = surahNumber,
            nameArabic = surahEntity?.nameArabic ?: QuranData.surahs[surahNumber - 1].nameArabic,
            nameTransliterated = surahEntity?.nameTransliterated
                ?: QuranData.surahs[surahNumber - 1].nameTransliterated,
            ayahs = ayahEntities.map { it.toAyah() }
        ).also { cache[surahNumber] = it }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun searchAyahs(query: String): Flow<List<AyahEntity>> =
        ayahDao.searchAyahs(query)

    fun searchSurahs(query: String) =
        surahDao.searchSurahs(query)

    // ── Ayah of the day ───────────────────────────────────────────────────────

    @OptIn(ExperimentalTime::class)
    suspend fun getAyahOfTheDay(): Ayah {
        val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())
        val seed = (today.year * 10000L + today.monthNumber * 100 + today.dayOfMonth)
        return ayahDao.getAyahOfTheDay(seed).toAyah()
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

}