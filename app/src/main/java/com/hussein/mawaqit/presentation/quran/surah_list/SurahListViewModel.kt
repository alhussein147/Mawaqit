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
            workerManager.getWorkInfosForUniqueWorkFlow(QURAN_TAFSIR_WORK_NAME)
                .collect { infos ->
                    if (infos.isEmpty()) return@collect

                    val failedInfo = infos.find { it.state == WorkInfo.State.FAILED || it.state == WorkInfo.State.CANCELLED }
                    if (failedInfo != null) {
                        val error = failedInfo.outputData.getString("error")
                            ?: failedInfo.progress.getString("error")
                            ?: "Failed to download Quran data. Please check your network connection and try again."
                        _syncStatus.update { it.copy(isSyncing = false, error = error) }
                        return@collect
                    }

                    val isAnyActive = infos.any { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    if (isAnyActive) {
                        val quranInfo = infos.find { it.tags.contains(QURAN_SYNC_TAG) }
                        val tafsirInfo = infos.find { it.tags.contains(TAFSIR_SYNC_TAG) }
                        val audioInfo = infos.find { it.tags.contains(AUDIO_SOURCE_SYNC_TAG) }

                        var totalProgress = 0f
                        if (quranInfo != null) {
                            val p0 = quranInfo.progress.getFloat(GenericPopulationWorker.KEY_PROGRESS, 0f)
                            if (quranInfo.state == WorkInfo.State.SUCCEEDED) totalProgress += 1f / 3f
                            else if (quranInfo.state == WorkInfo.State.RUNNING) totalProgress += p0 / 3f
                        }
                        if (tafsirInfo != null) {
                            val p1 = tafsirInfo.progress.getFloat(GenericPopulationWorker.KEY_PROGRESS, 0f)
                            if (tafsirInfo.state == WorkInfo.State.SUCCEEDED) totalProgress += 1f / 3f
                            else if (tafsirInfo.state == WorkInfo.State.RUNNING) totalProgress += p1 / 3f
                        }
                        if (audioInfo != null) {
                            val p2 = audioInfo.progress.getFloat(GenericPopulationWorker.KEY_PROGRESS, 0f)
                            if (audioInfo.state == WorkInfo.State.SUCCEEDED) totalProgress += 1f / 3f
                            else if (audioInfo.state == WorkInfo.State.RUNNING) totalProgress += p2 / 3f
                        }
                        _syncStatus.update { it.copy(isSyncing = true, progress = totalProgress, error = null) }
                        return@collect
                    }

                    val isAllSucceeded = infos.all { it.state == WorkInfo.State.SUCCEEDED }
                    if (isAllSucceeded) {
                        _syncStatus.update { it.copy(isSyncing = false, progress = 1f, error = null) }
                    }
                }
        }
    }

    fun syncQuranData() {
        viewModelScope.launch {
            _syncStatus.update { it.copy(isSyncing = true, error = null, progress = 0f) }

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
                QURAN_TAFSIR_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                quranRequest
            ).then(tafsirRequest)
                .then(audioSourceRequest)
                .enqueue()
        }
    }

    fun syncAudioSources() {
        val audioSourceRequest = OneTimeWorkRequestBuilder<GenericPopulationWorker>()
            .addTag(AUDIO_SOURCE_SYNC_TAG)
            .setInputData(workDataOf(GenericPopulationWorker.KEY_STRATEGY_NAME to "AudioSource"))
            .build()

        workerManager.enqueueUniqueWork(
            AUDIO_SOURCE_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            audioSourceRequest
        )
    }

    companion object {
        private const val QURAN_TAFSIR_WORK_NAME = "quran_tafsir_population"
        private const val AUDIO_SOURCE_SYNC_WORK_NAME = "audio_source_population_sync"
        private const val QURAN_SYNC_TAG = "quran_sync"
        private const val TAFSIR_SYNC_TAG = "tafsir_sync"
        private const val AUDIO_SOURCE_SYNC_TAG = "audio_source_sync"
    }
}
