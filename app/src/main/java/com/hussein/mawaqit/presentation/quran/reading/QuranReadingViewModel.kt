package com.hussein.mawaqit.presentation.quran.reading

import android.util.Log
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hussein.mawaqit.data.RecitationRepository
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.domain.models.Ayah
import com.hussein.mawaqit.domain.models.Bookmark
import com.hussein.mawaqit.domain.models.SurahDetail
import com.hussein.mawaqit.infrastructure.connectivity.NetworkObserver
import com.hussein.mawaqit.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.settings.QuranTextAlignment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
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

@Stable
data class QuranReaderScreenState(
    val readerState: QuranReaderUiState = QuranReaderUiState.Idle,
    val fontSize: Float = 18f,
    val textAlignment: QuranTextAlignment = QuranTextAlignment.Center,
    val bookmarks: List<Bookmark> = emptyList(),
    val selectedAyah: Ayah? = null,
    val tafsirState: TafsirState = TafsirState.Idle,
    val recitationState: AyahRecitationState = AyahRecitationState.Idle,
    val playingAyah: Int? = null,
    val selectedReciter: AudioSourceEntity? = null,
    val availableReciters: List<AudioSourceEntity> = emptyList(),
    val isNetworkAvailable: Boolean = true
)

class QuranReadingViewModel(
    private val recitationRepository: RecitationRepository,
    private val tafsirRepository: TafsirRepository,
    private val quranReaderPreferences: QuranReaderPreferences,
    private val networkObserver: NetworkObserver,
    private val ayahPlayer: AyahPlayer,
    private val quranDatabaseRepository: QuranDatabaseRepository
) : ViewModel() {

    private val TAG = "ReaderScreenViewModel"
    
    private val textAlignment = quranReaderPreferences.quranTextAlignment
    private val fontSize = quranReaderPreferences.fontSizeFlow
    private val _readerState = MutableStateFlow<QuranReaderUiState>(QuranReaderUiState.Idle)
    private val networkAvailable = networkObserver.networkAvailableFlow()
    private val bookmarks = quranDatabaseRepository.getAllBookmarks()
    
    private val ayahRecitationState = ayahPlayer.state
    private val playingAyah = ayahPlayer.playingAyah
    private val _selectedReciter = MutableStateFlow<AudioSourceEntity?>(null)
    private val _availableReciters = recitationRepository.getAyahReciters()
    private val _tafsirState = MutableStateFlow<TafsirState>(TafsirState.Idle)
    private val _selectedAyah = MutableStateFlow<Ayah?>(null)

    val uiState: StateFlow<QuranReaderScreenState> = combine(
        textAlignment,
        fontSize,
        _readerState,
        networkAvailable,
        bookmarks,
        ayahRecitationState,
        playingAyah,
        _selectedReciter,
        _availableReciters,
        _tafsirState,
        _selectedAyah
    ) { args ->
        QuranReaderScreenState(
            textAlignment = args[0] as QuranTextAlignment,
            fontSize = args[1] as Float,
            readerState = args[2] as QuranReaderUiState,
            isNetworkAvailable = args[3] as Boolean,
            bookmarks = args[4] as List<Bookmark>,
            recitationState = args[5] as AyahRecitationState,
            playingAyah = args[6] as Int?,
            selectedReciter = args[7] as AudioSourceEntity?,
            availableReciters = args[8] as List<AudioSourceEntity>,
            tafsirState = args[9] as TafsirState,
            selectedAyah = args[10] as Ayah?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuranReaderScreenState()
    )

    private var loadedSurahIndex = -1

    init {
        viewModelScope.launch {
            _availableReciters.filter { it.isNotEmpty() }.first().let { list ->
                if (_selectedReciter.value == null) {
                    _selectedReciter.value = list.firstOrNull { it.name.contains("ياسر") } ?: list.firstOrNull()
                }
            }
        }
    }

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

    fun toggleBookmark(surahNumber: Int, ayahNumber: Int, surahName: String) {
        viewModelScope.launch {
            quranDatabaseRepository.toggleBookmark(
                surahName = surahName,
                surahNumber = surahNumber,
                ayahNumber = ayahNumber
            )
        }
    }

    fun playAyah(surahNumber: Int, ayahNumber: Int) {
        val reciter = _selectedReciter.value ?: return
        val url = recitationRepository.ayahUrl(reciter, surahNumber, ayahNumber)
        if (url == null) {
            Log.e(TAG, "playAyah: URL is null for reciter ${reciter.name}")
            return
        }
        Log.d(TAG, "playAyah:$url ")
        ayahPlayer.play(
            url = url,
            ayahNumber = ayahNumber
        )
        updateLastRead(surahNumber, ayahNumber)
    }

    fun stopAyah() {
        ayahPlayer.stop()
    }

    fun selectReciter(reciter: AudioSourceEntity) {
        stopAyah()
        _selectedReciter.value = reciter
    }

    fun fetchTafsir(surahIndex: Int, ayah: Ayah) {
        viewModelScope.launch {
            _tafsirState.value = TafsirState.Loading
            val sourceId = tafsirRepository.selectedTafsirSourceId.first()
            val tafsir = tafsirRepository.fetchTafsirForAyah(sourceId ?: "mukhtasar", surahIndex, ayah.numberInSurah)
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
            Log.d(TAG, "saved last read ayah: ${ayahIndex} surah: $surahIndex")
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
