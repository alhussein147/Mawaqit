package com.hussein.mawaqit.presentation.quran

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirRepository
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirState
import com.hussein.mawaqit.data.network.networkAvailableFlow
import com.hussein.mawaqit.data.quran.Ayah
import com.hussein.mawaqit.data.quran.QuranRepository
import com.hussein.mawaqit.data.quran.SurahDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable


private val Context.quranDataStore by preferencesDataStore("quran_prefs")

enum class QuranFontSize(val sp: Int, val label: String) {
    SMALL(18, "S"),
    MEDIUM(22, "M"),
    LARGE(26, "L"),
    XLARGE(30, "XL")
}

enum class QuranTextAlignment(val displayName: String) {
    Start("Start"), Center("Center"), End("End")
}

@Serializable
data class QuranBookmark(
    val surahIndex: Int,
    val ayahNumber: Int
)

sealed interface QuranReaderState {
    data object Idle : QuranReaderState
    data object Loading : QuranReaderState
    data class Success(val surah: SurahDetail) : QuranReaderState
    data class Error(val message: String) : QuranReaderState
}

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private val KEY_FONT_SIZE = stringPreferencesKey("quran_font_size")
        private val KEY_TEXT_ALIGNMENT = stringPreferencesKey("quran_text_alignment")
        private val KEY_BM_SURAH = intPreferencesKey("quran_bm_surah")
        private val KEY_BM_AYAH = intPreferencesKey("quran_bm_ayah")
    }


    private val repo = QuranRepository(application)
    private val ds = application.quranDataStore
    private val tafsirRepo: TafsirRepository = TafsirRepository()



    // ── Reader state ──────────────────────────────────────────────────────────

    private val _readerState = MutableStateFlow<QuranReaderState>(QuranReaderState.Idle)
    val readerState: StateFlow<QuranReaderState> = _readerState.asStateFlow()

    // ── Network state ─────────────────────────────────────────────────────────

    val networkAvailable: StateFlow<Boolean> = networkAvailableFlow(context = application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var loadedSurahIndex = -1

    fun loadSurah(index: Int) {
        if (loadedSurahIndex == index && _readerState.value is QuranReaderState.Success) return
        viewModelScope.launch {
            _readerState.value = QuranReaderState.Loading
            _readerState.value = try {
                QuranReaderState.Success(repo.loadSurah(index)).also { loadedSurahIndex = index }
            } catch (e: Exception) {
                QuranReaderState.Error("Failed to load surah: ${e.message}")
            }
        }
    }

    val quranTextAlignment: StateFlow<QuranTextAlignment> =
        MutableStateFlow(QuranTextAlignment.Center).also { flow ->
            viewModelScope.launch {
                ds.data.map { prefs ->
                    prefs[KEY_TEXT_ALIGNMENT]?.let { runCatching { QuranTextAlignment.valueOf(it) }.getOrNull() }
                        ?: QuranTextAlignment.Center
                }.collect { flow.value = it }
            }
        }

    fun setTextAlignment(alignment: QuranTextAlignment) {
        viewModelScope.launch {
            ds.edit { it[KEY_TEXT_ALIGNMENT] = alignment.name }
        }
    }

    // ── Font size ─────────────────────────────────────────────────────────────
    val fontSize: StateFlow<QuranFontSize> = MutableStateFlow(QuranFontSize.MEDIUM).also { flow ->
        viewModelScope.launch {
            ds.data.map { prefs ->
                prefs[KEY_FONT_SIZE]?.let { runCatching { QuranFontSize.valueOf(it) }.getOrNull() }
                    ?: QuranFontSize.MEDIUM
            }.collect { flow.value = it }
        }
    }

    fun setFontSize(size: QuranFontSize) {
        viewModelScope.launch {
            ds.edit { it[KEY_FONT_SIZE] = size.name }
        }
    }

    // ── Bookmark ──────────────────────────────────────────────────────────────
    val bookmark: StateFlow<QuranBookmark?> = MutableStateFlow<QuranBookmark?>(null).also { flow ->
        viewModelScope.launch {
            ds.data.map { prefs ->
                val s = prefs[KEY_BM_SURAH] ?: return@map null
                val a = prefs[KEY_BM_AYAH] ?: return@map null
                QuranBookmark(s, a)
            }.collect { flow.value = it }
        }
    }

    fun setBookmark(surahIndex: Int, ayahNumber: Int) {
        viewModelScope.launch {
            ds.edit {
                it[KEY_BM_SURAH] = surahIndex
                it[KEY_BM_AYAH] = ayahNumber
            }
        }
    }

    fun clearBookmark() {
        viewModelScope.launch {
            ds.edit {
                it.remove(KEY_BM_SURAH)
                it.remove(KEY_BM_AYAH)
            }
        }
    }

    // ── Tafsir ────────────────────────────────────────────────────────────────

    private val _tafsirState  = MutableStateFlow<TafsirState>(TafsirState.Idle)
    val tafsirState: StateFlow<TafsirState> = _tafsirState.asStateFlow()

    private val _selectedAyah = MutableStateFlow<Ayah?>(null)
    val selectedAyah: StateFlow<Ayah?> = _selectedAyah.asStateFlow()

    fun fetchTafsir(surahIndex: Int, ayah: Ayah) {
        _selectedAyah.value = ayah
        if (!networkAvailable.value) {
            _tafsirState.value = TafsirState.NoNetwork
            return
        }
        viewModelScope.launch {
            _tafsirState.value = TafsirState.Loading
            _tafsirState.value = try {
                TafsirState.Success(tafsirRepo.fetchTafsir(surahIndex, ayah.number))
            } catch (e: Exception) {
                TafsirState.Error("Error loading tafisr")
            }
        }
    }

    fun dismissTafsir() {
        _tafsirState.value  = TafsirState.Idle
        _selectedAyah.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tafsirRepo.close()
    }

}