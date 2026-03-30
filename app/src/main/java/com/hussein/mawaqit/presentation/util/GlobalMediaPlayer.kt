package com.hussein.mawaqit.presentation.util

import android.app.Application
import android.content.ComponentName
import android.content.Context
import androidx.lifecycle.AndroidViewModel
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
import com.hussein.mawaqit.data.infrastructure.services.GlobalPlayerService
import com.hussein.mawaqit.data.infrastructure.services.PlaybackSource
import com.hussein.mawaqit.data.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.data.quran.recitation.FullSurahReciter
import com.hussein.mawaqit.data.quran.recitation.RecitationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.collections.toMutableMap

sealed interface SurahItemState {
    data object NotDownloaded : SurahItemState
    data class Downloading(val progress: Float, val workId: UUID) : SurahItemState
    data object Downloaded : SurahItemState
    data object Playing : SurahItemState
    data object Paused : SurahItemState
}

class GlobalPlayerViewModel(
    application: Application,
    private val recitationRepository: RecitationRepository,
    private val workManager: WorkManager
) : AndroidViewModel(application) {


    // ── Reciter ───────────────────────────────────────────────────────────────
    private val _selectedReciter = MutableStateFlow(FullSurahReciter.Husr)
    val selectedReciter: StateFlow<FullSurahReciter> = _selectedReciter.asStateFlow()

    // ── Playback source ───────────────────────────────────────────────────────
    private val _source = MutableStateFlow<PlaybackSource>(PlaybackSource.None)
    val source: StateFlow<PlaybackSource> = _source.asStateFlow()

    // ── Surah download states ─────────────────────────────────────────────────
    private val _surahStates = MutableStateFlow<Map<Int, SurahItemState>>(emptyMap())
    val surahStates: StateFlow<Map<Int, SurahItemState>> = _surahStates.asStateFlow()

    // ── Is global player currently playing ───────────────────────────────────
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        initMediaController(application)
        refreshDownloadStates()
    }

    // ── MediaController ───────────────────────────────────────────────────────

    private fun initMediaController(context: Context) {
        val token = SessionToken(context, ComponentName(context, GlobalPlayerService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            mediaController = future.get().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        syncSurahPlayingState(isPlaying)
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) stop()
                    }
                })
            }
        }, MoreExecutors.directExecutor())
    }

    // ── Surah playback ────────────────────────────────────────────────────────

    fun playSurah(surahNumber: Int) {
        val file = recitationRepository.surahFile(_selectedReciter.value, surahNumber)
        if (!file.exists()) return
        _source.value = PlaybackSource.Surah(surahNumber)
        playUri(file.toURI().toString())
    }

    // ── Radio playback ────────────────────────────────────────────────────────

    fun playRadio(stationUrl: String) {
        _source.value = PlaybackSource.Radio(stationUrl)
        playUri(stationUrl)
    }

    // ── Common controls ───────────────────────────────────────────────────────

    fun togglePlayPause() {
        mediaController?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun stop() {
        mediaController?.stop()
        _source.value = PlaybackSource.None
        _isPlaying.value = false
        refreshDownloadStates()
    }

    private fun playUri(uri: String) {
        mediaController?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            play()
        }
    }

    // ── Download ──────────────────────────────────────────────────────────────

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
                        surahNumber, SurahItemState.Downloading(
                            info.progress.getFloat(SurahDownloadWorker.KEY_PROGRESS, 0f), request.id
                        )
                    )

                    WorkInfo.State.SUCCEEDED -> updateSurahState(
                        surahNumber, SurahItemState.Downloaded
                    )

                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> updateSurahState(
                        surahNumber, SurahItemState.NotDownloaded
                    )

                    else -> Unit
                }
            }
        }
    }

    fun cancelDownload(workId: UUID) = workManager.cancelWorkById(workId)

    // ── Reciter ───────────────────────────────────────────────────────────────

    fun selectReciter(reciter: FullSurahReciter) {
        if (_selectedReciter.value == reciter) return
        stop()
        _selectedReciter.value = reciter
        refreshDownloadStates()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun refreshDownloadStates() {
        viewModelScope.launch {
            _surahStates.value = (1..114).associate { n ->
                n to if (recitationRepository.isSurahCached(
                        _selectedReciter.value,
                        n
                    )
                ) SurahItemState.Downloaded else SurahItemState.NotDownloaded
            }
        }
    }

    private fun updateSurahState(surahNumber: Int, state: SurahItemState) {
        _surahStates.value = _surahStates.value.toMutableMap().apply { put(surahNumber, state) }
    }

    private fun syncSurahPlayingState(isPlaying: Boolean) {
        val src = _source.value
        if (src is PlaybackSource.Surah) {
            updateSurahState(
                src.surahNumber, if (isPlaying) SurahItemState.Playing else SurahItemState.Paused
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
    }
}