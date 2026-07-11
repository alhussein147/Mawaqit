package com.hussein.mawaqit.presentation.azkar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.hussein.mawaqit.data.azkar.AzkarRepository
import com.hussein.mawaqit.data.db.entities.AzkarCategoryEntity
import com.hussein.mawaqit.data.db.entities.AzkarItemEntity
import com.hussein.mawaqit.infrastructure.connectivity.NetworkObserver
import com.hussein.mawaqit.infrastructure.workers.local_population_workers.GenericPopulationWorker
import com.hussein.mawaqit.presentation.shared.SyncStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AzkarUiState(
    val categories: List<AzkarCategoryEntity> = emptyList(),
    val currentCategory: AzkarCategoryEntity? = null,
    val items: List<AzkarItemEntity> = emptyList(),
    val isLoadingItems: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgress: Float = 0f,
    val isOffline: Boolean = false,
    val error: String? = null,
    val itemsError: String? = null
)

class AzkarViewModel(
    private val azkarRepository: AzkarRepository,
    private val workerManager: WorkManager,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val _syncState = MutableStateFlow(SyncStatus())
    private val _currentCategoryId = MutableStateFlow<Int?>(null)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AzkarUiState> = combine(
        azkarRepository.getAllCategories(),
        _syncState,
        networkObserver.networkAvailableFlow(),
        _currentCategoryId
    ) { categories, sync, isAvailable, categoryId ->
        AzkarUiState(
            categories = categories,
            currentCategory = categories.find { it.id == categoryId },
            isSyncing = sync.isSyncing,
            syncProgress = sync.progress,
            isOffline = !isAvailable,
            error = sync.error
        )
    }.flatMapLatest { state ->
        val categoryId = state.currentCategory?.id
        if (categoryId == null) {
            flowOf(state)
        } else {
            azkarRepository.getItemsForCategory(categoryId)
                .map { items -> state.copy(items = items, isLoadingItems = false) }
                .onStart { emit(state.copy(isLoadingItems = true)) }
                .catch { e -> emit(state.copy(isLoadingItems = false, itemsError = e.message)) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AzkarUiState()
    )

    init {
        observeSyncProgress()
    }

    private fun observeSyncProgress() {
        viewModelScope.launch {
            workerManager.getWorkInfosForUniqueWorkFlow(AZKAR_SYNC_WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                            val progress = info.progress.getFloat(GenericPopulationWorker.KEY_PROGRESS, 0f)
                            _syncState.update { it.copy(isSyncing = true, progress = progress, error = null) }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _syncState.update { it.copy(isSyncing = false, progress = 1f) }
                        }
                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString("error") ?: "Sync failed"
                            _syncState.update { it.copy(isSyncing = false, error = error) }
                        }
                        else -> {
                            _syncState.update { it.copy(isSyncing = false) }
                        }
                    }
                }
        }
    }

    fun syncAzkar() {
        if (uiState.value.isOffline) return
        
        workerManager.enqueueUniqueWork(
            AZKAR_SYNC_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<GenericPopulationWorker>()
                .setInputData(workDataOf(GenericPopulationWorker.KEY_STRATEGY_NAME to "Azkar"))
                .build()
        )
    }

    fun selectCategory(categoryId: Int) {
        _currentCategoryId.value = categoryId
    }

    companion object {
        private const val AZKAR_SYNC_WORK_NAME = "azkar_population_sync"
    }
}
