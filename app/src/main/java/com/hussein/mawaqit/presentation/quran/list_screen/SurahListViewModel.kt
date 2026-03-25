package com.hussein.mawaqit.presentation.quran.list_screen

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.util.Log
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
import com.hussein.mawaqit.data.infrastructure.services.SurahPlayerService
import com.hussein.mawaqit.data.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.data.recitation.FullSurahReciter
import com.hussein.mawaqit.data.recitation.RecitationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

// Per-surah state shown in the list



class SurahListViewModel(application: Application) : AndroidViewModel(application) {

    private val recitationRepository = RecitationRepository(application)
    private val workManager = WorkManager.getInstance(application)

    // Session-only reciter selection
    private val _selectedReciter = MutableStateFlow(FullSurahReciter.Husr)
    val selectedReciter: StateFlow<FullSurahReciter> = _selectedReciter.asStateFlow()

    // Map of surahNumber → state
    private val _surahStates = MutableStateFlow<Map<Int, SurahItemState>>(emptyMap())
    val surahStates: StateFlow<Map<Int, SurahItemState>> = _surahStates.asStateFlow()

    // Currently playing surah number
    private val _playingSurah = MutableStateFlow<Int?>(null)
    val playingSurah: StateFlow<Int?> = _playingSurah.asStateFlow()

    private var mediaController: MediaController? = null

    init {
        initMediaController(application)
        refreshDownloadStates()
    }

    // ── MediaController ───────────────────────────────────────────────────────

    private fun initMediaController(context: Context) {
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

    // ── Download state ────────────────────────────────────────────────────────

    private fun refreshDownloadStates() {
        viewModelScope.launch {
            val states = (1..114).associate { surahNumber ->
                surahNumber to if (recitationRepository.isSurahCached(_selectedReciter.value, surahNumber))
                    SurahItemState.Downloaded
                else
                    SurahItemState.NotDownloaded
            }
            _surahStates.value = states
        }
    }

    fun downloadSurah(surahNumber: Int) {
        val workName = workName(surahNumber)
        val request = OneTimeWorkRequestBuilder<SurahDownloadWorker>()
            .setInputData(SurahDownloadWorker.inputData(surahNumber, _selectedReciter.value.id))
            .build()

        workManager.enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, request)

        // Observe WorkInfo for this specific request
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(request.id).collect { info ->
                when (info?.state) {
                    WorkInfo.State.RUNNING -> {
                        val progress = info.progress
                            .getFloat(SurahDownloadWorker.KEY_PROGRESS, 0f)
                        updateSurahState(
                            surahNumber,
                            SurahItemState.Downloading(progress, request.id)
                        )
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        updateSurahState(surahNumber, SurahItemState.Downloaded)
                    }

                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED -> {
                        updateSurahState(surahNumber, SurahItemState.NotDownloaded)
                    }

                    else -> Unit
                }
            }
        }
    }

    fun cancelDownload(workId: UUID) {
        workManager.cancelWorkById(workId)
    }

    // ── Playback ──────────────────────────────────────────────────────────────

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

    fun stop() {
        mediaController?.stop()
        _playingSurah.value = null
        refreshDownloadStates()
    }

    // ── Reciter ───────────────────────────────────────────────────────────────

    fun selectReciter(reciter: FullSurahReciter) {
        if (_selectedReciter.value == reciter) return
        stop()
        _selectedReciter.value = reciter
        refreshDownloadStates()
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun workName(surahNumber: Int) =
        "surah_download_${_selectedReciter.value.id}_$surahNumber"

    private fun updateSurahState(surahNumber: Int, state: SurahItemState) {
        _surahStates.value = _surahStates.value.toMutableMap().apply {
            put(surahNumber, state)
        }
    }

    private fun updatePlayingState(isPlaying: Boolean) {
        val surah = _playingSurah.value ?: return
        updateSurahState(
            surah,
            if (isPlaying) SurahItemState.Playing else SurahItemState.Paused
        )
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        mediaController?.release()
        mediaController?.stop()
    }

}