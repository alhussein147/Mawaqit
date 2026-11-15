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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.common.util.concurrent.MoreExecutors
import com.hussein.mawaqit.data.RecitationRepository
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.domain.models.Surah
import com.hussein.mawaqit.infrastructure.services.GlobalPlayerService
import com.hussein.mawaqit.infrastructure.services.PlaybackSource
import com.hussein.mawaqit.infrastructure.workers.SurahDownloadWorker
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

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
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val currentSurah: Surah? = null,
    val errorMessage: String? = null,
    val availableReciters: List<AudioSourceEntity> = emptyList()
)

class GlobalPlayerViewModel(
    application: Application,
    private val recitationRepository: RecitationRepository,
    private val quranDatabaseRepository: QuranDatabaseRepository,
    private val workManager: WorkManager
) : AndroidViewModel(application) {

    private companion object {
        const val TAG = "GlobalPlayerViewModel"
    }

    val selectedReciter = MutableStateFlow<AudioSourceEntity?>(null)

    private val source = MutableStateFlow<PlaybackSource>(PlaybackSource.None)

    private val _surahStates = MutableStateFlow<Map<Int, SurahItemState>>(emptyMap())
    val surahStates: StateFlow<Map<Int, SurahItemState>> = _surahStates.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(GlobalPlaybackUiState())
    val playbackState: StateFlow<GlobalPlaybackUiState> = _playbackState.asStateFlow()

    private val _allSurahs = MutableStateFlow<List<Surah>>(emptyList())

    private var mediaController: MediaController? = null
    private var pendingMediaItem: Pair<PlaybackSource, MediaItem>? = null
    private var progressJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            _playbackState.value = _playbackState.value.copy(isPlaying = isPlaying)
            syncSurahPlayingState(isPlaying)
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
                updatePositionOnce()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _playbackState.value = _playbackState.value.copy(
                isBuffering = playbackState == Player.STATE_BUFFERING
            )
            updatePositionOnce()

            if (playbackState == Player.STATE_ENDED) {
                stop()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Global media playback failed", error)
            stopProgressUpdates()
            val failedSource = source.value
            mediaController?.stop()
            pendingMediaItem = null
            source.value = PlaybackSource.None
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
        observeAvailableReciters()
        observeSurahs()
    }

    private fun observeSurahs() {
        viewModelScope.launch {
            quranDatabaseRepository.getAllSurahs().collect { list ->
                _allSurahs.value = list
                updateCurrentSurah(source.value)
            }
        }
    }

    private fun updateCurrentSurah(source: PlaybackSource) {
        val surah = if (source is PlaybackSource.Surah) {
            _allSurahs.value.find { it.number == source.surahNumber }
        } else null
        _playbackState.value = _playbackState.value.copy(currentSurah = surah)
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                updatePositionOnce()
                delay(500L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun updatePositionOnce() {
        val controller = mediaController ?: return
        val pos = controller.currentPosition.coerceAtLeast(0L)
        val dur = controller.duration.coerceAtLeast(0L)
        _playbackState.value = _playbackState.value.copy(
            currentPositionMs = pos,
            durationMs = if (dur > 0) dur else _playbackState.value.durationMs
        )
    }

    private fun observeAvailableReciters() {
        viewModelScope.launch {
            recitationRepository.getSurahReciters().collect { list ->
                _playbackState.value = _playbackState.value.copy(availableReciters = list)
                if (selectedReciter.value == null && list.isNotEmpty()) {
                    selectedReciter.value = list.firstOrNull { it.name.contains("الحصري") } ?: list.firstOrNull()
                    refreshDownloadStates()
                }
            }
        }
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
                        if (controller.isPlaying) {
                            startProgressUpdates()
                        }
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
        val reciter = selectedReciter.value ?: return
        val file = recitationRepository.surahFile(reciter.id, surahNumber)
        if (!file.exists()) {
            clearPlaybackError()
            updateSurahState(surahNumber, SurahItemState.NotDownloaded)
            return
        }

        val source = PlaybackSource.Surah(surahNumber)
        val mediaItem = MediaItem.Builder()
            .setUri(Uri.fromFile(file))
            .setMediaId("surah:${reciter.id}:$surahNumber")
            .build()
        playMediaItem(source, mediaItem)
    }

    fun playRadio(stationUrl: String, title: String) {
        val cleanedUrl = stationUrl.trim()
        if (cleanedUrl.isBlank()) {
            _playbackState.value = _playbackState.value.copy(errorMessage = "Radio URL is empty")
            return
        }

        val source = PlaybackSource.Radio(cleanedUrl)
        val mediaItem = MediaItem.Builder()
            .setUri(cleanedUrl)
            .setMediaId(title)
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

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPositionMs = positionMs)
    }

    fun playNextSurah() {
        val currentSource = source.value
        if (currentSource is PlaybackSource.Surah) {
            val nextSurah = currentSource.surahNumber + 1
            if (nextSurah <= 114) {
                playSurah(nextSurah)
            }
        }
    }

    fun playPreviousSurah() {
        val currentSource = source.value
        if (currentSource is PlaybackSource.Surah) {
            val prevSurah = currentSource.surahNumber - 1
            if (prevSurah >= 1) {
                playSurah(prevSurah)
            }
        }
    }

    fun stop() {
        stopProgressUpdates()
        val previousSource = source.value
        pendingMediaItem = null
        mediaController?.stop()
        source.value = PlaybackSource.None
        _isPlaying.value = false
        _playbackState.value = _playbackState.value.copy(
            source = PlaybackSource.None,
            isPlaying = false,
            isBuffering = false,
            currentPositionMs = 0L,
            durationMs = 0L,
            currentSurah = null
        )
        clearSourceState(previousSource)
    }

    fun clearPlaybackError() {
        if (_playbackState.value.errorMessage != null) {
            _playbackState.value = _playbackState.value.copy(errorMessage = null)
        }
    }

    private fun playMediaItem(source: PlaybackSource, mediaItem: MediaItem) {
        clearSourceState(source)
        this.source.value = source
        val surah = if (source is PlaybackSource.Surah) {
            _allSurahs.value.find { it.number == source.surahNumber }
        } else null
        _playbackState.value = _playbackState.value.copy(
            source = source,
            isBuffering = true,
            currentPositionMs = 0L,
            durationMs = 0L,
            currentSurah = surah,
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
        val reciter = selectedReciter.value ?: return
        val uniqueWorkName = "surah_download_${reciter.id}_$surahNumber"
        val request = OneTimeWorkRequestBuilder<SurahDownloadWorker>().setInputData(
            SurahDownloadWorker.inputData(
                surahNumber,
                reciter.id
            )
        ).build()

        workManager.enqueueUniqueWork(
            uniqueWorkName,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )

        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(uniqueWorkName).collect { infos ->
                val info = infos.firstOrNull()
                when (info?.state) {
                    WorkInfo.State.RUNNING -> updateSurahState(
                        surahNumber,
                        SurahItemState.Downloading(
                            info.progress.getFloat(SurahDownloadWorker.KEY_PROGRESS, 0f),
                            info.id
                        )
                    )

                    WorkInfo.State.SUCCEEDED -> {
                        updateSurahState(
                            surahNumber,
                            SurahItemState.Downloaded
                        )
                        cancel()
                    }

                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> {
                        updateSurahState(
                            surahNumber,
                            SurahItemState.NotDownloaded
                        )
                        cancel()
                    }

                    else -> Unit
                }
            }
        }
    }

    fun cancelDownload(workId: UUID) = workManager.cancelWorkById(workId)

    fun selectReciter(reciter: AudioSourceEntity) {
        if (selectedReciter.value?.id == reciter.id) return
        stop()
        selectedReciter.value = reciter
        refreshDownloadStates()
    }

    private fun refreshDownloadStates() {
        val reciter = selectedReciter.value ?: return
        viewModelScope.launch {
            val currentSource = source.value
            val isCurrentSurahPlaying = _isPlaying.value
            _surahStates.value = (1..114).associate { surahNumber ->
                surahNumber to when {
                    currentSource is PlaybackSource.Surah &&
                            currentSource.surahNumber == surahNumber &&
                            recitationRepository.isSurahCached(
                                reciter.id,
                                surahNumber
                            ) ->
                        if (isCurrentSurahPlaying) SurahItemState.Playing else SurahItemState.Paused

                    recitationRepository.isSurahCached(reciter.id, surahNumber) ->
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
        val source = source.value
        if (source is PlaybackSource.Surah) {
            updateSurahState(
                source.surahNumber,
                if (isPlaying) SurahItemState.Playing else SurahItemState.Paused
            )
        }
    }

    private fun clearSourceState(source: PlaybackSource) {
        val reciter = selectedReciter.value ?: return
        if (source is PlaybackSource.Surah) {
            updateSurahState(
                source.surahNumber,
                if (recitationRepository.isSurahCached(
                        reciter.id,
                        source.surahNumber
                    )
                ) {
                    SurahItemState.Downloaded
                } else {
                    SurahItemState.NotDownloaded
                }
            )
        }
    }

    override fun onCleared() {
        stopProgressUpdates()
        mediaController?.removeListener(playerListener)
        mediaController?.release()
        mediaController = null
        super.onCleared()
    }
}
