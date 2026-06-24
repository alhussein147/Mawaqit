package com.hussein.mawaqit.presentation.quran.reader

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.data.quran.QuranDisplayPreferences
import com.hussein.mawaqit.data.quran.QuranTextAlignment
import com.hussein.mawaqit.data.quran.recitation.RecitationRepository
import com.hussein.mawaqit.data.quran.recitation.Reciter
import com.hussein.mawaqit.data.quran.tafsir.TafsirRepository
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.SurahDetail
import com.hussein.mawaqit.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.infrastructure.network.NetworkObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
sealed interface AyahRecitationState {
    data object Idle : AyahRecitationState
    data object Buffering : AyahRecitationState
    data object Playing : AyahRecitationState
}

@Stable
sealed interface TafsirState {
    data object Idle : TafsirState
    data object Loading : TafsirState
    data class Success(val text: String) : TafsirState
    data class Error(val message: String) : TafsirState
    data object NoNetwork : TafsirState
}
@Stable
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

    val textAlignment: StateFlow<QuranTextAlignment> = quranDisplayPreferences.quranTextAlignment
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranTextAlignment.Center)

    fun setTextAlignment(alignment: QuranTextAlignment) {
        viewModelScope.launch { quranDisplayPreferences.setTextAlignmentSize(alignment) }
    }

    // font size prefs
    val fontSize: StateFlow<Float> = quranDisplayPreferences.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 18f)

    fun setFontSize(size: Float) {
        viewModelScope.launch { quranDisplayPreferences.setFontSize(size) }
    }

    private val _readerState = MutableStateFlow<QuranReaderUiState>(QuranReaderUiState.Idle)
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

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

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch { quranDatabaseRepository.toggleBookmark(surahNumber, ayahNumber) }
    }

    val ayahRecitationState: StateFlow<AyahRecitationState> = ayahPlayer.state
    val playingAyah: StateFlow<Int?> = ayahPlayer.playingAyah

    private val _selectedReciter = MutableStateFlow(Reciter.YASSER)
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