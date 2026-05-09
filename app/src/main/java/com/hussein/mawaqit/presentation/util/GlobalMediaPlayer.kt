package com.hussein.mawaqit.presentation.util

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.MoreExecutors
import com.hussein.mawaqit.data.infrastructure.services.GlobalPlayerService
import com.hussein.mawaqit.data.infrastructure.services.PlaybackSource
import com.hussein.mawaqit.data.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.data.quran.recitation.FullSurahReciter
import com.hussein.mawaqit.data.quran.recitation.RecitationRepository
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SurahItemState {
    data object NotDownloaded : SurahItemState
    data class Downloading(val progress: Float, val workId: UUID) : SurahItemState
    data object Downloaded : SurahItemState
    data object Playing : SurahItemState
    data object Paused : SurahItemState
}

data class GlobalPlaybackUiState(
    val source: PlaybackSource = PlaybackSource.None,
    val isControllerReady: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null
)

class GlobalPlayerViewModel(
    application: Application,
    private val recitationRepository: RecitationRepository,
    private val workManager: WorkManager
) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "GlobalPlayerViewModel"
    }

    private val _selectedReciter = MutableStateFlow(FullSurahReciter.Husr)
    val selectedReciter: StateFlow<FullSurahReciter> = _selectedReciter.asStateFlow()

    private val _source = MutableStateFlow<PlaybackSource>(PlaybackSource.None)
    val source: StateFlow<PlaybackSource> = _source.asStateFlow()

    private val _surahStates = MutableStateFlow<Map<Int, SurahItemState>>(emptyMap())
    val surahStates: StateFlow<Map<Int, SurahItemState>> = _surahStates.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(GlobalPlaybackUiState())
    val playbackState: StateFlow<GlobalPlaybackUiState> = _playbackState.asStateFlow()

    private var mediaController: MediaController? = null
    private var pendingMediaItem: Pair<PlaybackSource, MediaItem>? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            syncSurahPlayingState(isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = _playbackState.value.copy(
                isBuffering = playbackState == Player.STATE_BUFFERING
            )

            if (playbackState == Player.STATE_ENDED) {
                stop()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Global media playback failed", error)
            val failedSource = _source.value
            mediaController?.stop()
            pendingMediaItem = null
            _source.value = PlaybackSource.None
            _isPlaying.value = false
            _playbackState.value = GlobalPlaybackUiState(
                isControllerReady = mediaController != null,
                errorMessage = error.localizedMessage ?: "Unable to play this audio"
            )
            clearSourceState(failedSource)
        }
    }

    init {
        initMediaController(application)
        refreshDownloadStates()
    }

    private fun initMediaController(context: Context) {
        val token = SessionToken(context, ComponentName(context, GlobalPlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { controller ->
                        mediaController = controller.apply { addListener(playerListener) }
                        _isPlaying.value = controller.isPlaying
                        _playbackState.value = _playbackState.value.copy(
                            isControllerReady = true,
                            isPlaying = controller.isPlaying,
                            isBuffering = controller.playbackState == Player.STATE_BUFFERING,
                            errorMessage = null
                        )
                        pendingMediaItem?.let { (source, mediaItem) ->
                            pendingMediaItem = null
                            playMediaItem(source, mediaItem)
                        }
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Failed to connect to global media session", error)
                        _playbackState.value = _playbackState.value.copy(
                            isControllerReady = false,
                            errorMessage = "Unable to connect to audio player"
                        )
                    }
            },
            MoreExecutors.directExecutor()
        )
    }

    fun playSurah(surahNumber: Int) {
        val file = recitationRepository.surahFile(_selectedReciter.value, surahNumber)
        if (!file.exists()) {
            clearPlaybackError()
            updateSurahState(surahNumber, SurahItemState.NotDownloaded)
            return
        }

        val source = PlaybackSource.Surah(surahNumber)
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId("surah:${_selectedReciter.value.id}:$surahNumber")
            .build()
        playMediaItem(source, mediaItem)
    }

    fun playRadio(stationUrl: String) {
        val cleanedUrl = stationUrl.trim()
        if (cleanedUrl.isBlank()) {
            _playbackState.value = _playbackState.value.copy(errorMessage = "Radio URL is empty")
            return
        }

        val source = PlaybackSource.Radio(cleanedUrl)
        val mediaItem = MediaItem.Builder()
            .setUri(cleanedUrl)
            .setMediaId("radio:$cleanedUrl")
            .build()
        playMediaItem(source, mediaItem)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        clearPlaybackError()
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun stop() {
        val previousSource = _source.value
        pendingMediaItem = null
        mediaController?.stop()
        _source.value = PlaybackSource.None
        _isPlaying.value = false
        _playbackState.value = GlobalPlaybackUiState(isControllerReady = mediaController != null)
        clearSourceState(previousSource)
    }

    fun clearPlaybackError() {
        if (_playbackState.value.errorMessage != null) {
            _playbackState.value = _playbackState.value.copy(errorMessage = null)
        }
    }

    private fun playMediaItem(source: PlaybackSource, mediaItem: MediaItem) {
        clearSourceState(_source.value)
        _source.value = source
        _playbackState.value = _playbackState.value.copy(
            source = source,
            isBuffering = true,
            errorMessage = null
        )

        val controller = mediaController
        if (controller == null) {
            pendingMediaItem = source to mediaItem
            return
        }

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
    }

    fun downloadSurah(surahNumber: Int) {
        val request = OneTimeWorkRequestBuilder<SurahDownloadWorker>().setInputData(
            SurahDownloadWorker.inputData(
                surahNumber,
                _selectedReciter.value.id
            )
        ).build()

        workManager.enqueueUniqueWork(
            "surah_download_${_selectedReciter.value.id}_$surahNumber",
            ExistingWorkPolicy.KEEP,
            request
        )

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                when (info?.state) {
                    WorkInfo.State.RUNNING -> updateSurahState(
                        surahNumber,
                        SurahItemState.Downloading(
                            info.progress.getFloat(SurahDownloadWorker.KEY_PROGRESS, 0f),
                            request.id
                        )
                    )

                    WorkInfo.State.SUCCEEDED -> updateSurahState(
                        surahNumber,
                        SurahItemState.Downloaded
                    )

                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> updateSurahState(
                        surahNumber,
                        SurahItemState.NotDownloaded
                    )

                    else -> Unit
                }
            }
        }
    }

    fun cancelDownload(workId: UUID) = workManager.cancelWorkById(workId)

    fun selectReciter(reciter: FullSurahReciter) {
        if (_selectedReciter.value == reciter) return
        stop()
        _selectedReciter.value = reciter
        refreshDownloadStates()
    }

    private fun refreshDownloadStates() {
        viewModelScope.launch {
            val currentSource = _source.value
            val isCurrentSurahPlaying = _isPlaying.value
            _surahStates.value = (1..114).associate { surahNumber ->
                surahNumber to when {
                    currentSource is PlaybackSource.Surah &&
                        currentSource.surahNumber == surahNumber &&
                        recitationRepository.isSurahCached(_selectedReciter.value, surahNumber) ->
                        if (isCurrentSurahPlaying) SurahItemState.Playing else SurahItemState.Paused

                    recitationRepository.isSurahCached(_selectedReciter.value, surahNumber) ->
                        SurahItemState.Downloaded

                    else -> SurahItemState.NotDownloaded
                }
            }
        }
    }

    private fun updateSurahState(surahNumber: Int, state: SurahItemState) {
        _surahStates.value = _surahStates.value.toMutableMap().apply {
            put(surahNumber, state)
        }
    }

    private fun syncSurahPlayingState(isPlaying: Boolean) {
        val source = _source.value
        if (source is PlaybackSource.Surah) {
            updateSurahState(
                source.surahNumber,
                if (isPlaying) SurahItemState.Playing else SurahItemState.Paused
            )
        }
    }

    private fun clearSourceState(source: PlaybackSource) {
        if (source is PlaybackSource.Surah) {
            updateSurahState(
                source.surahNumber,
                if (recitationRepository.isSurahCached(_selectedReciter.value, source.surahNumber)) {
                    SurahItemState.Downloaded
                } else {
                    SurahItemState.NotDownloaded
                }
            )
        }
    }

    override fun onCleared() {
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }
}
