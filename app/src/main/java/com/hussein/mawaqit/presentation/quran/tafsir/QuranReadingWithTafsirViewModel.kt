package com.hussein.mawaqit.presentation.quran.tafsir

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.domain.models.AyahWithTafsir
import com.hussein.mawaqit.infrastructure.workers.population_workers.GenericPopulationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface QuranTafsirUiState {
    data object Loading : QuranTafsirUiState
    data class Success(val ayahs: List<AyahWithTafsir>) : QuranTafsirUiState
    data class Error(val message: String) : QuranTafsirUiState
}

class QuranReadingWithTafsirViewModel(
    private val tafsirRepository: TafsirRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<QuranTafsirUiState>(QuranTafsirUiState.Loading)
    val uiState: StateFlow<QuranTafsirUiState> = _uiState.asStateFlow()

    val availableSources: StateFlow<List<TafsirSourceEntity>> = tafsirRepository.getAvailableTafsirSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedSourceId: StateFlow<String?> = tafsirRepository.selectedTafsirSourceId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _downloadingSources = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadingSources: StateFlow<Map<String, Int>> = _downloadingSources.asStateFlow()

    private var currentSurahNumber: Int? = null

    init {
        viewModelScope.launch {
            tafsirRepository.syncTafsirSources()
        }
        
        observeDownloadProgress()

        // Reload when selection changes
        viewModelScope.launch {
            selectedSourceId.collect { id ->
                currentSurahNumber?.let { loadTafsir(it) }
            }
        }
    }

    private fun observeDownloadProgress() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow("tafsir_sync").collect { workInfos ->
                val progressMap = workInfos.filter { it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED }
                    .associate { info ->
                        val sourceId = info.tags.find { it.startsWith("tafsir_sync_") }?.removePrefix("tafsir_sync_") ?: ""
                        val progress = info.progress.getInt(GenericPopulationWorker.KEY_PROGRESS, 0)
                        sourceId to progress
                    }
                _downloadingSources.value = progressMap
            }
        }
    }

    fun loadTafsir(surahNumber: Int) {
        currentSurahNumber = surahNumber
        viewModelScope.launch {
            _uiState.value = QuranTafsirUiState.Loading
            try {
                val sourceId = selectedSourceId.value ?: "mukhtasar"
                val data = tafsirRepository.fetchSurahWithTafsir(sourceId, surahNumber)
                _uiState.value = QuranTafsirUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = QuranTafsirUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectTafsirSource(source: TafsirSourceEntity) {
        viewModelScope.launch {
            if (!source.downloaded) {
                downloadTafsir(source)
                return@launch
            }
            tafsirRepository.setSelectedTafsirSourceId(source.id)
        }
    }

    private fun downloadTafsir(source: TafsirSourceEntity) {
        val workRequest = OneTimeWorkRequestBuilder<GenericPopulationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString(GenericPopulationWorker.KEY_STRATEGY_NAME, "Tafsir")
                    .putString(GenericPopulationWorker.KEY_SOURCE_ID, source.id)
                    .build()
            )
            .addTag("tafsir_sync")
            .addTag("tafsir_sync_${source.id}")
            .build()

        workManager.enqueue(workRequest)
    }
}
