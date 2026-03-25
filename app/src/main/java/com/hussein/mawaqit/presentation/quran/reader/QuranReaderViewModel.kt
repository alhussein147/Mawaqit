package com.hussein.mawaqit.presentation.quran.reader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.models.Ayah
import com.hussein.mawaqit.data.db.models.SurahDetail
import com.hussein.mawaqit.data.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.data.infrastructure.network.NetworkObserver
import com.hussein.mawaqit.data.quran.QuranDisplayPreferences
import com.hussein.mawaqit.data.quran.QuranFontSize
import com.hussein.mawaqit.data.quran.QuranTextAlignment
import com.hussein.mawaqit.data.recitation.RecitationRepository
import com.hussein.mawaqit.data.recitation.Reciter
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AyahRecitationState {
    data object Idle : AyahRecitationState
    data object Buffering : AyahRecitationState
    data object Playing : AyahRecitationState
}


sealed interface TafsirState {
    data object Idle : TafsirState
    data object Loading : TafsirState
    data class Success(val text: String) : TafsirState
    data class Error(val message: String) : TafsirState
    data object NoNetwork : TafsirState
}

sealed interface QuranReaderUiState {
    data object Idle : QuranReaderUiState
    data object Loading : QuranReaderUiState
    data class Success(val surah: SurahDetail) : QuranReaderUiState
    data class Error(val message: String) : QuranReaderUiState
}

class QuranViewModel(
    private val recitationRepository: RecitationRepository,
    private val tafsirRepository: TafsirRepository,
    private val quranDisplayPreferences: QuranDisplayPreferences,
    private val networkObserver: NetworkObserver,
    private val ayahPlayer: AyahPlayer,
    private val quranDatabaseRepository: QuranDatabaseRepository
) : ViewModel() {

    // quran text alignment

    val textAlignment: StateFlow<QuranTextAlignment> = quranDisplayPreferences.quranTextAlignment
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranTextAlignment.Center)

    fun setTextAlignment(alignment: QuranTextAlignment) {
        viewModelScope.launch { quranDisplayPreferences.setTextAlignmentSize(alignment) }
    }

    // ── Font size — delegated to repo ─────────────────────────────────────────
    val fontSize: StateFlow<QuranFontSize> = quranDisplayPreferences.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranFontSize.MEDIUM)

    fun setFontSize(size: QuranFontSize) {
        viewModelScope.launch { quranDisplayPreferences.setFontSize(size) }
    }

    // ── Reader state ──────────────────────────────────────────────────────────

    private val _readerState = MutableStateFlow<QuranReaderUiState>(QuranReaderUiState.Idle)
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    // ── Network state ─────────────────────────────────────────────────────────

    val networkAvailable: StateFlow<Boolean> = networkObserver.networkAvailableFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)


    private var loadedSurahIndex = -1

    fun loadSurah(index: Int) {
        if (loadedSurahIndex == index && _readerState.value is QuranReaderUiState.Success) return
        viewModelScope.launch {
            _readerState.value = QuranReaderUiState.Loading
            _readerState.value = try {
                QuranReaderUiState.Success(quranDatabaseRepository.loadSurah(index))
                    .also { loadedSurahIndex = index }
            } catch (e: Exception) {
                QuranReaderUiState.Error("Failed to load surah: ${e.message}")
            }
        }
    }

    val bookmarks = quranDatabaseRepository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun isBookmarked(surahNumber: Int, ayahNumber: Int) =
        quranDatabaseRepository.isBookmarked(surahNumber, ayahNumber)

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch { quranDatabaseRepository.toggleBookmark(surahNumber, ayahNumber) }
    }

    // ── Ayah recitation ───────────────────────────────────────────────────────

    val ayahRecitationState: StateFlow<AyahRecitationState> = ayahPlayer.state
    val playingAyah: StateFlow<Int?> = ayahPlayer.playingAyah

    private val _selectedReciter = MutableStateFlow(Reciter.MISHARY)
    val selectedReciter: StateFlow<Reciter> = _selectedReciter.asStateFlow()


    fun playAyah(surahNumber: Int, ayahNumber: Int) {
        val url = recitationRepository.ayahUrl(_selectedReciter.value, surahNumber, ayahNumber)
        Log.d("QuranReaderViewModel", "playAyah:$url ")
        ayahPlayer.play(
            url = url,
            ayahNumber = ayahNumber
        )
    }

    fun stopAyah() {
        ayahPlayer.stop()
    }

    fun selectReciter(reciter: Reciter) {
        stopAyah()
        _selectedReciter.value = reciter
    }


    // ── Tafsir ────────────────────────────────────────────────────────────────

    private val _tafsirState = MutableStateFlow<TafsirState>(TafsirState.Idle)
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
                TafsirState.Success(tafsirRepository.fetchTafsir(surahIndex, ayah.numberInSurah))
            } catch (e: Exception) {
                TafsirState.Error("Error loading tafisr")
            }
        }
    }

    fun selectAyah(ayah: Ayah) {
        _selectedAyah.value = ayah
        _tafsirState.value = TafsirState.Idle
    }

    fun dismissTafsir() {
        _tafsirState.value = TafsirState.Idle
        _selectedAyah.value = null
    }

    override fun onCleared() {
        super.onCleared()
        tafsirRepository.close()
        ayahPlayer.release()
    }

}