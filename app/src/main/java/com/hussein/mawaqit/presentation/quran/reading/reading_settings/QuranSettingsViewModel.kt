package com.hussein.mawaqit.presentation.quran.reading.reading_settings

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
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.settings.QuranTextAlignment
import com.hussein.mawaqit.infrastructure.workers.population_workers.GenericPopulationWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuranSettingsViewModel(
    private val quranReaderPreferences: QuranReaderPreferences,
    private val tafsirRepository: TafsirRepository,
    private val workManager: WorkManager
) : ViewModel() {

    private val _availableTafsirSources = MutableStateFlow<List<TafsirSourceEntity>>(emptyList())
    val availableTafsirSources: StateFlow<List<TafsirSourceEntity>> = _availableTafsirSources.asStateFlow()

    val selectedTafsirSourceId: StateFlow<String?> = tafsirRepository.selectedTafsirSourceId
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _quranLanguages = MutableStateFlow(listOf("Arabic", "English", "French", "Urdu")) // Extended placeholders
    val quranLanguages: StateFlow<List<String>> = _quranLanguages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("Arabic")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()


    val textAlignment: StateFlow<QuranTextAlignment> = quranReaderPreferences.quranTextAlignment
        .stateIn(viewModelScope, SharingStarted.Eagerly, QuranTextAlignment.Center)

    val fontSize: StateFlow<Float> = quranReaderPreferences.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 18f)

    private val _downloadingSources = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadingSources: StateFlow<Map<String, Int>> = _downloadingSources.asStateFlow()

    init {
        observeTafsirSources()
        observeDownloadProgress()
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

    private fun observeTafsirSources() {
        viewModelScope.launch {
            tafsirRepository.getAvailableTafsirSources().collect {
                _availableTafsirSources.value = it
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

    // TODO: handle langs 
    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
    }


    fun setTextAlignment(alignment: QuranTextAlignment) {
        viewModelScope.launch {
            quranReaderPreferences.setTextAlignmentSize(alignment)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            quranReaderPreferences.setFontSize(size)
        }
    }
}