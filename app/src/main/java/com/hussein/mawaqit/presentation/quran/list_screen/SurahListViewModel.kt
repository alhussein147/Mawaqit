package com.hussein.mawaqit.presentation.quran.list_screen

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.MoreExecutors
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import com.hussein.mawaqit.data.infrastructure.services.SurahPlayerService
import com.hussein.mawaqit.data.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.data.quran.recitation.FullSurahReciter
import com.hussein.mawaqit.data.quran.recitation.RecitationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID


sealed interface SurahItemState {
    data object NotDownloaded : SurahItemState
    data class Downloading(val progress: Float, val workId: UUID) : SurahItemState
    data object Downloaded : SurahItemState
    data object Playing : SurahItemState
    data object Paused : SurahItemState
}

class SurahPlayer(val context: Context, val recitationRepository: RecitationRepository) {

    private val _selectedReciter = MutableStateFlow(FullSurahReciter.Husr)
    val selectedReciter: StateFlow<FullSurahReciter> = _selectedReciter.asStateFlow()

    // Map of surahNumber → state
    private val _surahStates = MutableStateFlow<Map<Int, SurahItemState>>(emptyMap())
    val surahStates: StateFlow<Map<Int, SurahItemState>> = _surahStates.asStateFlow()

    // Currently playing surah number
    private val _playingSurah = MutableStateFlow<Int?>(null)
    val playingSurah: StateFlow<Int?> = _playingSurah.asStateFlow()

    fun initMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, SurahPlayerService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        future.addListener({
            mediaController = future.get().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayingState(isPlaying)
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) stop()
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    private var mediaController: MediaController? = null

    suspend fun refreshDownloadStates() {
        val states = (1..114).associate { surahNumber ->
            surahNumber to if (recitationRepository.isSurahCached(
                    _selectedReciter.value,
                    surahNumber
                )
            )
                SurahItemState.Downloaded
            else
                SurahItemState.NotDownloaded
        }
        _surahStates.value = states
    }

    fun playSurah(surahNumber: Int) {
        val file = recitationRepository.surahFile(_selectedReciter.value, surahNumber)
        Log.d("SurahListViewModel", "playSurah: $file")
        if (!file.exists()) return

        _playingSurah.value = surahNumber
        mediaController?.apply {
            setMediaItem(MediaItem.fromUri(file.toURI().toString()))
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    suspend fun selectReciter(reciter: FullSurahReciter) {
        if (_selectedReciter.value == reciter) return
        stop()
        _selectedReciter.value = reciter
        refreshDownloadStates()
    }

    suspend fun stop() {
        mediaController?.stop()
        _playingSurah.value = null
        refreshDownloadStates()
    }

    fun updateSurahState(surahNumber: Int, state: SurahItemState) {
        _surahStates.value = _surahStates.value.toMutableMap().apply {
            put(surahNumber, state)
        }
    }

    fun updatePlayingState(isPlaying: Boolean) {
        val surah = _playingSurah.value ?: return
        updateSurahState(
            surah,
            if (isPlaying) SurahItemState.Playing else SurahItemState.Paused
        )
    }

    fun release() {
        mediaController?.release()
        mediaController = null
    }

}

class SurahListViewModel(
    private val surahPlayer: SurahPlayer,
    private val quranDatabaseRepository: QuranDatabaseRepository,
    private val workManager: WorkManager
) : ViewModel() {


    // Session-only reciter selection
    val selectedReciter = surahPlayer.selectedReciter.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FullSurahReciter.Basit
    )

    val playingSurah = surahPlayer.playingSurah.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val surahStates = surahPlayer.surahStates.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    // Session-only reciter selection
    val bookmarks = quranDatabaseRepository.getAllBookmarks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        emptyList()
    )


    init {
        surahPlayer.initMediaController()
        viewModelScope.launch {
            surahPlayer.refreshDownloadStates()
        }
    }


    fun togglePlayPause() = surahPlayer.togglePlayPause()
    fun playSurah(surahNumber: Int) = surahPlayer.playSurah(surahNumber)

    fun selectedReciter(reciter: FullSurahReciter) {
        viewModelScope.launch {
            surahPlayer.selectReciter(reciter)
        }
    }

    fun deleteBookmark(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            quranDatabaseRepository.removeBookmark(surahNumber, ayahNumber)
        }
    }

    private fun workName(surahNumber: Int) =
        "surah_download_${selectedReciter.value.id}_$surahNumber"

    fun downloadSurah(surahNumber: Int) {
        val workName = workName(surahNumber)
        val request = OneTimeWorkRequestBuilder<SurahDownloadWorker>()
            .setInputData(SurahDownloadWorker.inputData(surahNumber, selectedReciter.value.id))
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)

        // Observe WorkInfo for this specific request
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                when (info?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress
                            .getFloat(SurahDownloadWorker.KEY_PROGRESS, 0f)
                        surahPlayer.updateSurahState(
                            surahNumber,
                            SurahItemState.Downloading(progress, request.id)
                        )
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        surahPlayer.updateSurahState(surahNumber, SurahItemState.Downloaded)
                    }

                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> {
                        surahPlayer.updateSurahState(surahNumber, SurahItemState.NotDownloaded)
                    }

                    else -> Unit
                }
            }
        }
    }

    fun cancelDownload(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    override fun onCleared() {
        super.onCleared()
        surahPlayer.release()
    }

}