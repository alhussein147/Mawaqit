package com.hussein.mawaqit.presentation.quran.reader

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.hussein.mawaqit.data.network.networkAvailableFlow
import com.hussein.mawaqit.data.quran.Ayah
import com.hussein.mawaqit.data.quran.QuranRepository
import com.hussein.mawaqit.data.recitation.RecitationRepository
import com.hussein.mawaqit.data.recitation.Reciter
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirRepository
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class QuranViewModel(application: Application) : AndroidViewModel(application) {

    private val recitationRepo = RecitationRepository(application)

    private val repo = QuranRepository(application)
    private val tafsirRepo: TafsirRepository = TafsirRepository()


    val textAlignment: StateFlow<QuranTextAlignment> = repo.quranTextAlignment
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranTextAlignment.Center)

    fun setTextAlignment(alignment: QuranTextAlignment) {
        viewModelScope.launch { repo.setTextAlignmentSize(alignment) }
    }

    // ── Font size — delegated to repo ─────────────────────────────────────────
    val fontSize: StateFlow<QuranFontSize> = repo.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranFontSize.MEDIUM)

    fun setFontSize(size: QuranFontSize) {
        viewModelScope.launch { repo.setFontSize(size) }
    }

    // ── Bookmark — delegated to repo ──────────────────────────────────────────
    val bookmark: StateFlow<QuranBookmark?> = repo.bookmarkFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setBookmark(surahIndex: Int, ayahNumber: Int) {
        viewModelScope.launch { repo.setBookmark(surahIndex, ayahNumber) }
    }

    fun clearBookmark() {
        viewModelScope.launch { repo.clearBookmark() }
    }

    // ── Reader state ──────────────────────────────────────────────────────────

    private val _readerState = MutableStateFlow<QuranReaderUiState>(QuranReaderUiState.Idle)
    val readerState: StateFlow<QuranReaderUiState> = _readerState.asStateFlow()

    // ── Network state ─────────────────────────────────────────────────────────

    val networkAvailable: StateFlow<Boolean> = networkAvailableFlow(context = application)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var loadedSurahIndex = -1

    fun loadSurah(index: Int) {
        if (loadedSurahIndex == index && _readerState.value is QuranReaderUiState.Success) return
        viewModelScope.launch {
            _readerState.value = QuranReaderUiState.Loading
            _readerState.value = try {
                QuranReaderUiState.Success(repo.loadSurah(index)).also { loadedSurahIndex = index }
            } catch (e: Exception) {
                QuranReaderUiState.Error("Failed to load surah: ${e.message}")
            }
        }
    }

    // ── Ayah recitation ───────────────────────────────────────────────────────
    private val _ayahRecitationState =
        MutableStateFlow<RecitationState>(RecitationState.Idle)
    val ayahRecitationState: StateFlow<RecitationState> = _ayahRecitationState.asStateFlow()

    private val _playingAyah = MutableStateFlow<Int?>(null)
    val playingAyah: StateFlow<Int?> = _playingAyah.asStateFlow()

    private val _selectedReciter = MutableStateFlow(Reciter.MISHARY)
    val selectedReciter: StateFlow<Reciter> = _selectedReciter.asStateFlow()

    private val ayahPlayer = ExoPlayer.Builder(application).build().apply {
        addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> _ayahRecitationState.value =
                        RecitationState.Buffering

                    Player.STATE_READY -> _ayahRecitationState.value =
                        if (isPlaying) RecitationState.Playing else RecitationState.Idle

                    Player.STATE_ENDED -> stopAyah()
                    else -> Unit
                }
            }
        })
    }

    fun playAyah(surahNumber: Int, ayahNumber: Int) {
        val url = recitationRepo.ayahUrl(_selectedReciter.value, surahNumber, ayahNumber)
        Log.d("QuranReaderViewModel", "playAyah:$url ")
        _playingAyah.value = ayahNumber
        ayahPlayer.setMediaItem(MediaItem.fromUri(url))
        ayahPlayer.prepare()
        ayahPlayer.play()
        _ayahRecitationState.value = RecitationState.Buffering
    }

    fun stopAyah() {
        ayahPlayer.stop()
        _playingAyah.value = null
        _ayahRecitationState.value = RecitationState.Idle
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
                TafsirState.Success(tafsirRepo.fetchTafsir(surahIndex, ayah.number))
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
        tafsirRepo.close()
        ayahPlayer.release()
    }

}