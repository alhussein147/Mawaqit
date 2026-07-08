package com.hussein.mawaqit.infrastructure.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class QuranTextAlignment(val displayName: String) {
    Start("Start"), Center("Center"), End("End")
}

val Context.quranDataStore by preferencesDataStore("quran_prefs")

class QuranReaderPreferences(private val context: Context) {


    companion object {
        private val KEY_FONT_SIZE = floatPreferencesKey("quran_font_size")
        private val KEY_TEXT_ALIGNMENT = stringPreferencesKey("quran_text_alignment")
        private val KEY_LAST_READ_SURAH = intPreferencesKey("last_read_surah")
        private val KEY_LAST_READ_AYAH = intPreferencesKey("last_read_ayah")
    }

    private val ds = context.quranDataStore

    val quranTextAlignment: Flow<QuranTextAlignment> = ds.data.map { prefs ->
        prefs[KEY_TEXT_ALIGNMENT]
            ?.let { runCatching { QuranTextAlignment.valueOf(it) }.getOrNull() }
            ?: QuranTextAlignment.End
    }

    val fontSizeFlow: Flow<Float> = ds.data.map { prefs ->
        prefs[KEY_FONT_SIZE] ?: 18f
    }

    val lastReadSurah: Flow<Int?> = ds.data.map { it[KEY_LAST_READ_SURAH] }
    val lastReadAyah: Flow<Int?> = ds.data.map { it[KEY_LAST_READ_AYAH] }


    suspend fun setTextAlignmentSize(alignment: QuranTextAlignment) {
        ds.edit { it[KEY_TEXT_ALIGNMENT] = alignment.name }
    }
    suspend fun setFontSize(size: Float) {
        ds.edit { it[KEY_FONT_SIZE] = size }
    }

    suspend fun setLastRead(surah: Int, ayah: Int) {
        ds.edit {
            it[KEY_LAST_READ_SURAH] = surah
            it[KEY_LAST_READ_AYAH] = ayah
        }
    }

    suspend fun setLastReadSurah(surah: Int) {
        ds.edit {
            if (it[KEY_LAST_READ_SURAH] != surah) {
                it[KEY_LAST_READ_SURAH] = surah
                it[KEY_LAST_READ_AYAH] = 1
            }
        }
    }


}