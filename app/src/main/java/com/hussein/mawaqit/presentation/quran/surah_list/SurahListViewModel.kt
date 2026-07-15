package com.hussein.mawaqit.presentation.quran.surah_list

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.domain.models.Surah
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.workers.population_workers.GenericPopulationWorker
import com.hussein.mawaqit.presentation.shared.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LastRead(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahName: String
)

@Stable
data class SurahListUiState(
    val isLoading: Boolean = true,
    val surahs: List<Surah> = emptyList(),
    val lastRead: LastRead? = null,
    val syncStatus: SyncStatus = SyncStatus(),
    val availableTafsirSources: List<TafsirSourceEntity> = emptyList(),
    val selectedTafsirSource: TafsirSourceEntity? = null,
    val quranLanguages: List<String> = listOf("Arabic"), // Hardcoded for now
    val selectedQuranLanguage: String = "Arabic"
)

class SurahListViewModel(
    quranDatabaseRepository: QuranDatabaseRepository,
    private val tafsirRepository: TafsirRepository,
    private val quranReaderPreferences: QuranReaderPreferences,
    private val workerManager: WorkManager
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    private val _syncStatus = MutableStateFlow(SyncStatus())
    private val _availableTafsirSources = MutableStateFlow<List<TafsirSourceEntity>>(emptyList())
    private val _selectedTafsirSource = MutableStateFlow<TafsirSourceEntity?>(null)

    private val _surahs = quranDatabaseRepository.getAllSurahs()
        .onEach { _isLoading.value = false }

    private val _lastReadPosition: StateFlow<LastRead?> = combine(
        _surahs,
        quranReaderPreferences.lastReadSurah,
        quranReaderPreferences.lastReadAyah
    ) { surahList, lastSurah, lastAyah ->
        if (lastSurah != null && lastAyah != null) {
            val surah = surahList.find { it.number == lastSurah }
            if (surah != null) {
                LastRead(lastSurah, lastAyah, surah.nameTransliterated)
            } else null
        } else null
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val uiState: StateFlow<SurahListUiState> = combine(
        _isLoading,
        _surahs,
        _lastReadPosition,
        _syncStatus,
        _availableTafsirSources,
        _selectedTafsirSource
    ) { args ->
        SurahListUiState(
            isLoading = args[0] as Boolean,
            surahs = args[1] as List<Surah>,
            lastRead = args[2] as LastRead?,
            syncStatus = args[3] as SyncStatus,
            availableTafsirSources = args[4] as List<TafsirSourceEntity>,
            selectedTafsirSource = args[5] as TafsirSourceEntity?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SurahListUiState()
    )

    init {
        observeSyncProgress()
        observeTafsirSources()
        observeSelectedTafsirSource()
    }

    private fun observeTafsirSources() {
        viewModelScope.launch {
            tafsirRepository.getAvailableTafsirSources().collect { entities ->
                if (entities.isEmpty()) {
                    tafsirRepository.syncTafsirSources()
                } else {
                    _availableTafsirSources.value = entities
                    _selectedTafsirSource.value = entities.find { it.isActive }
                }
            }
        }
    }

    private fun observeSelectedTafsirSource() {
        viewModelScope.launch {
            tafsirRepository.selectedTafsirSourceId.collect { id ->
                _selectedTafsirSource.value = _availableTafsirSources.value.find { it.id == id }
            }
        }
    }

    fun selectTafsirSource(source: TafsirSourceEntity) {
        _selectedTafsirSource.value = source
        viewModelScope.launch {
            tafsirRepository.setSelectedTafsirSourceId(source.id)
        }
    }

    private fun observeSyncProgress() {
        viewModelScope.launch {
            // We'll observe by tags since we are chaining
            workerManager.getWorkInfosByTagFlow(QURAN_SYNC_TAG)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    updateSyncState(info, stage = 0)
                }
        }
        viewModelScope.launch {
            workerManager.getWorkInfosByTagFlow(TAFSIR_SYNC_TAG)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    if (info.state != WorkInfo.State.ENQUEUED) {
                        updateSyncState(info, stage = 1)
                    }
                }
        }
        viewModelScope.launch {
            workerManager.getWorkInfosByTagFlow(AUDIO_SOURCE_SYNC_TAG)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    if (info.state != WorkInfo.State.ENQUEUED) {
                        updateSyncState(info, stage = 2)
                    }
                }
        }
    }

    private fun updateSyncState(info: WorkInfo, stage: Int) {
        val totalStages = 3
        when (info.state) {
            WorkInfo.State.RUNNING -> {
                val progress = info.progress.getFloat(GenericPopulationWorker.KEY_PROGRESS, 0f)
                val totalProgress = (stage + progress) / totalStages
                _syncStatus.update { it.copy(isSyncing = true, progress = totalProgress, error = null) }
            }
            WorkInfo.State.SUCCEEDED -> {
                if (stage == totalStages - 1) {
                    _syncStatus.update { it.copy(isSyncing = false, progress = 1f) }
                }
            }
            WorkInfo.State.FAILED -> {
                val error = info.outputData.getString("error") ?: "Sync failed"
                _syncStatus.update { it.copy(isSyncing = false, error = error) }
            }
            else -> {}
        }
    }

    fun syncQuranData() {
        viewModelScope.launch {
            val quranRequest = OneTimeWorkRequestBuilder<GenericPopulationWorker>()
                .addTag(QURAN_SYNC_TAG)
                .setInputData(workDataOf(GenericPopulationWorker.KEY_STRATEGY_NAME to "Quran"))
                .build()

            val sourceId = tafsirRepository.selectedTafsirSourceId.first()
            val tafsirRequest = OneTimeWorkRequestBuilder<GenericPopulationWorker>()
                .addTag(TAFSIR_SYNC_TAG)
                .setInputData(
                    workDataOf(
                        GenericPopulationWorker.KEY_STRATEGY_NAME to "Tafsir",
                        GenericPopulationWorker.KEY_SOURCE_ID to sourceId
                    )
                )
                .build()

            val audioSourceRequest = OneTimeWorkRequestBuilder<GenericPopulationWorker>()
                .addTag(AUDIO_SOURCE_SYNC_TAG)
                .setInputData(workDataOf(GenericPopulationWorker.KEY_STRATEGY_NAME to "AudioSource"))
                .build()

            workerManager.beginUniqueWork(
                "quran_tafsir_population",
                ExistingWorkPolicy.KEEP,
                quranRequest
            ).then(tafsirRequest)
                .then(audioSourceRequest)
                .enqueue()
        }
    }

    companion object {
        private const val QURAN_SYNC_TAG = "quran_sync"
        private const val TAFSIR_SYNC_TAG = "tafsir_sync"
        private const val AUDIO_SOURCE_SYNC_TAG = "audio_source_sync"
    }
}
