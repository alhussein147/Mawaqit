package com.hussein.mawaqit.presentation.quran.reader

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.data.quran.recitation.SurahDownloadRepository
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.Reciter
import com.hussein.mawaqit.domain.models.SurahDetail
import com.hussein.mawaqit.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.infrastructure.connectivity.NetworkObserver
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.settings.QuranTextAlignment
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
}

@Stable
sealed interface QuranReaderUiState {
    data object Idle : QuranReaderUiState
    data object Loading : QuranReaderUiState
    data class Success(val surah: SurahDetail) : QuranReaderUiState
    data class Error(val message: String) : QuranReaderUiState
}

class QuranViewModel(
    private val surahDownloadRepository: SurahDownloadRepository,
    private val tafsirRepository: TafsirRepository,
    private val quranReaderPreferences: QuranReaderPreferences,
    private val networkObserver: NetworkObserver,
    private val ayahPlayer: AyahPlayer,
    private val quranDatabaseRepository: QuranDatabaseRepository
) : ViewModel() {

    val textAlignment: StateFlow<QuranTextAlignment> = quranReaderPreferences.quranTextAlignment
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranTextAlignment.Center)


    // font size prefs
    val fontSize: StateFlow<Float> = quranReaderPreferences.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 18f)

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
                    .also { 
                        loadedSurahIndex = index
                        viewModelScope.launch {
                            quranReaderPreferences.setLastReadSurah(index)
                        }
                    }
            } catch (e: Exception) {
                QuranReaderUiState.Error("Failed to load surah: ${e.message}")
            }
        }
    }

    val bookmarks = quranDatabaseRepository.getAllBookmarks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int, surahName: String) {
        viewModelScope.launch {
            quranDatabaseRepository.toggleBookmark(
                surahName = surahName,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber
            )
        }
    }

    val ayahRecitationState: StateFlow<AyahRecitationState> = ayahPlayer.state
    val playingAyah: StateFlow<Int?> = ayahPlayer.playingAyah

    private val _selectedReciter = MutableStateFlow(Reciter.YASSER)
    val selectedReciter: StateFlow<Reciter> = _selectedReciter.asStateFlow()


    fun playAyah(surahNumber: Int, ayahNumber: Int) {
        val url = surahDownloadRepository.ayahUrl(_selectedReciter.value, surahNumber, ayahNumber)
        Log.d("QuranReaderViewModel", "playAyah:$url ")
        ayahPlayer.play(
            url = url,
            ayahNumber = ayahNumber
        )
        updateLastRead(surahNumber, ayahNumber)
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
        viewModelScope.launch {
            _tafsirState.value = TafsirState.Loading
            val tafsir = tafsirRepository.fetchTafsir(surahIndex, ayah.numberInSurah)
            if (tafsir != null) {
                _tafsirState.value = TafsirState.Success(tafsir.text)
            } else {
                _tafsirState.value = TafsirState.Error("Failed to fetch tafsir")
            }
        }
    }

    fun selectAyah(ayah: Ayah) {
        _selectedAyah.value = ayah
        _tafsirState.value = TafsirState.Idle
        updateLastRead(loadedSurahIndex, ayah.numberInSurah)
    }

    fun updateLastRead(surahIndex: Int, ayahIndex: Int) {
        viewModelScope.launch {
            quranReaderPreferences.setLastRead(surahIndex, ayahIndex)
        }
    }

    fun dismissTafsir() {
        _tafsirState.value = TafsirState.Idle
        _selectedAyah.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ayahPlayer.release()
    }

}