package com.hussein.mawaqit.presentation.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hussein.mawaqit.data.RecitationRepository
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.infrastructure.workers.population_workers.GenericPopulationWorker
import com.hussein.mawaqit.presentation.shared.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RadioUiState(
    val channels: List<AudioSourceEntity> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus()
)

class RadioViewModel(
    private val recitationRepository: RecitationRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _syncStatus = MutableStateFlow(SyncStatus())

    val uiState: StateFlow<RadioUiState> = combine(
        recitationRepository.getRadioSources(),
        _syncStatus
    ) { channels, sync ->
        RadioUiState(
            channels = channels,
            syncStatus = sync
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RadioUiState()
    )

    init {
        observeSyncProgress()
    }

    private fun observeSyncProgress() {
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(AUDIO_SOURCE_SYNC_WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                            val progress = info.progress.getFloat(GenericPopulationWorker.KEY_PROGRESS, 0f)
                            _syncStatus.update { 
                                it.copy(isSyncing = true, progress = progress, error = null) 
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _syncStatus.update { 
                                it.copy(isSyncing = false, progress = 1f) 
                            }
                        }
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString("error") ?: "Sync failed"
                            _syncStatus.update { 
                                it.copy(isSyncing = false, error = error) 
                            }
                        }
                        else -> {
                            _syncStatus.update { it.copy(isSyncing = false) }
                        }
                    }
                }
        }
    }

    fun syncAudioSources() {
        workManager.enqueueUniqueWork(
            AUDIO_SOURCE_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<GenericPopulationWorker>()
                .setInputData(workDataOf(GenericPopulationWorker.KEY_STRATEGY_NAME to "AudioSource"))
                .build()
        )
    }

    companion object {
        private const val AUDIO_SOURCE_SYNC_WORK_NAME = "audio_source_population_sync"
    }
}
