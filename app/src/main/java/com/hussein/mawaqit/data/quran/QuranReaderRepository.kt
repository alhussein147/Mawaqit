package com.hussein.mawaqit.data.quran

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hussein.mawaqit.presentation.quran.reader.QuranBookmark
import com.hussein.mawaqit.presentation.quran.reader.QuranFontSize
import com.hussein.mawaqit.presentation.quran.reader.QuranTextAlignment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Ayah(
    val number: Int,
    val text: String
)

data class SurahDetail(
    val index: Int,
    val name: String,
    val ayahs: List<Ayah>,
    val count: Int,
    val juzMap: Map<Int, Int> // ayah number → juz number
)

val Context.quranDataStore by preferencesDataStore("quran_prefs")

class QuranRepository(private val context: Context) {


    companion object {
        private val KEY_FONT_SIZE = stringPreferencesKey("quran_font_size")
        private val KEY_TEXT_ALIGNMENT = stringPreferencesKey("quran_text_alignment")
        private val KEY_BM_SURAH = intPreferencesKey("quran_bm_surah")
        private val KEY_BM_AYAH = intPreferencesKey("quran_bm_ayah")
    }

    private val ds   = context.quranDataStore
    private val json = Json { ignoreUnknownKeys = true }

    // LRU-style cache — max 2 surahs in memory at a time
    private val cache = object : LinkedHashMap<Int, SurahDetail>(3, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Int, SurahDetail>) = size > 2
    }

    suspend fun loadSurah(surahIndex: Int): SurahDetail = withContext(Dispatchers.IO) {
        cache[surahIndex]?.let { return@withContext it }

        val fileName = "quran/${surahIndex.toString().padStart(3, '0')}.json"
        val raw = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val root = json.parseToJsonElement(raw).jsonObject

        val verseObj = root["verse"]!!.jsonObject
        val ayahs = verseObj.keys
            .sortedBy { it.removePrefix("verse_").toInt() }
            .map { key ->
                Ayah(
                    number = key.removePrefix("verse_").toInt(),
                    text = verseObj[key]!!.jsonPrimitive.content
                )
            }

        // Build ayah→juz map from the juz array
        val juzMap = mutableMapOf<Int, Int>()
        (root["juz"] as? kotlinx.serialization.json.JsonArray)?.forEach { entry ->
            try {
                val obj = entry.jsonObject
                val juzNum = obj["index"]!!.jsonPrimitive.content.toInt()
                val startKey = obj["verse"]!!.jsonObject["start"]!!.jsonPrimitive.content
                val endKey = obj["verse"]!!.jsonObject["end"]!!.jsonPrimitive.content
                val startAyah = startKey.removePrefix("verse_").toInt()
                val endAyah = endKey.removePrefix("verse_").toInt()
                for (n in startAyah..endAyah) juzMap[n] = juzNum
            } catch (_: Exception) { /* malformed entry — skip */
            }
        }

        SurahDetail(
            index = root["index"]!!.jsonPrimitive.content.toInt(),
            name = root["name"]!!.jsonPrimitive.content,
            ayahs = ayahs,
            count = root["count"]!!.jsonPrimitive.content.toInt(),
            juzMap = juzMap
        ).also { cache[surahIndex] = it }
    }


    val quranTextAlignment: Flow<QuranTextAlignment> = ds.data.map { prefs ->
        prefs[KEY_TEXT_ALIGNMENT]
            ?.let { runCatching { QuranTextAlignment.valueOf(it) }.getOrNull() }
            ?: QuranTextAlignment.End
    }

    suspend fun setTextAlignmentSize(alignment: QuranTextAlignment) {
        ds.edit { it[KEY_TEXT_ALIGNMENT] = alignment.name }
    }

    // ── Font size ─────────────────────────────────────────────────────────────

    val fontSizeFlow: Flow<QuranFontSize> = ds.data.map { prefs ->
        prefs[KEY_FONT_SIZE]
            ?.let { runCatching { QuranFontSize.valueOf(it) }.getOrNull() }
            ?: QuranFontSize.MEDIUM
    }

    suspend fun setFontSize(size: QuranFontSize) {
        ds.edit { it[KEY_FONT_SIZE] = size.name }
    }

    // ── Bookmark ──────────────────────────────────────────────────────────────
    val bookmarkFlow: Flow<QuranBookmark?> = ds.data.map { prefs ->
        val s = prefs[KEY_BM_SURAH] ?: return@map null
        val a = prefs[KEY_BM_AYAH]  ?: return@map null
        QuranBookmark(s, a)
    }

    suspend fun setBookmark(surahIndex: Int, ayahNumber: Int) {
        ds.edit {
            it[KEY_BM_SURAH] = surahIndex
            it[KEY_BM_AYAH]  = ayahNumber
        }
    }

    suspend fun clearBookmark() {
        ds.edit {
            it.remove(KEY_BM_SURAH)
            it.remove(KEY_BM_AYAH)
        }
    }

}