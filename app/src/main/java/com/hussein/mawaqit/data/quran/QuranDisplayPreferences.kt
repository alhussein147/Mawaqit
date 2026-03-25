package com.hussein.mawaqit.data.quran

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


enum class QuranFontSize(val sp: Int, val label: String) {
    SMALL(18, "S"),
    MEDIUM(22, "M"),
    LARGE(26, "L"),
    XLARGE(30, "XL")
}

enum class QuranTextAlignment(val displayName: String) {
    Start("Start"), Center("Center"), End("End")
}

val Context.quranDataStore by preferencesDataStore("quran_prefs")

class QuranDisplayPreferences(private val context: Context) {


    companion object {
        private val KEY_FONT_SIZE = stringPreferencesKey("quran_font_size")
        private val KEY_TEXT_ALIGNMENT = stringPreferencesKey("quran_text_alignment")
    }

    private val ds = context.quranDataStore

    val quranTextAlignment: Flow<QuranTextAlignment> = ds.data.map { prefs ->
        prefs[KEY_TEXT_ALIGNMENT]
            ?.let { runCatching { QuranTextAlignment.valueOf(it) }.getOrNull() }
            ?: QuranTextAlignment.End
    }

    val fontSizeFlow: Flow<QuranFontSize> = ds.data.map { prefs ->
        prefs[KEY_FONT_SIZE]
            ?.let { runCatching { QuranFontSize.valueOf(it) }.getOrNull() }
            ?: QuranFontSize.MEDIUM
    }


    suspend fun setTextAlignmentSize(alignment: QuranTextAlignment) {
        ds.edit { it[KEY_TEXT_ALIGNMENT] = alignment.name }
    }
    suspend fun setFontSize(size: QuranFontSize) {
        ds.edit { it[KEY_FONT_SIZE] = size.name }
    }


}