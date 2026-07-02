package com.hussein.mawaqit.presentation.onboarding

import CurrentLocationFetcher
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.LocationRepository
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.infrastructure.network.NetworkObserver
import com.hussein.mawaqit.infrastructure.settings.AppSettings
import com.hussein.mawaqit.infrastructure.settings.AppTheme
import com.hussein.mawaqit.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.infrastructure.workers.DatabasePopulationWorker
import com.hussein.mawaqit.infrastructure.workers.DatabasePopulationWorker.Companion.DATABASE_POPULATION_WORK_NAME
import com.hussein.mawaqit.infrastructure.workers.QuranPopulationWorker
import com.hussein.mawaqit.infrastructure.workers.TafsirPopulationWorker
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val isLoadingLocation: Boolean = false,
    val savedLocation: SavedLocation? = null,
    val errorMessage: String? = null,
    val populationProgress: Float = 0f,
    val isQuranPopulating: Boolean = false,
    val quranPopulationFailed: Boolean = false,
    val isLocationGranted: Boolean = false,
    val isNotificationGranted: Boolean = false,
    val isExactAlarmGranted: Boolean = false,
    val isBatteryOptimizationIgnored: Boolean = false,
    val isOffline: Boolean = false,
    val settings: AppSettings? = null
)

class OnboardingViewModel(
    private val locationRepo: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val locationFetcher: CurrentLocationFetcher,
    private val workerManager: WorkManager,
    private val prayerSchedulerManager: PrayerSchedulerManager,
    private val networkObserver: NetworkObserver
) : ViewModel() {

    private val TAG = "OnboardingViewModel"
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        observeDatabasePopulation()
        observeNetwork()
        observeSettings()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkObserver.networkAvailableFlow().collect { isAvailable ->
                _uiState.update { it.copy(isOffline = !isAvailable) }
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeDatabasePopulation() {
        viewModelScope.launch {
            workerManager.getWorkInfosForUniqueWorkFlow(DATABASE_POPULATION_WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED -> {
                            val progress = info.progress
                                .getFloat(DatabasePopulationWorker.DATABASE_POPULATION_PROGRESS, 0f)
                            _uiState.update {
                                it.copy(
                                    populationProgress = progress,
                                    quranPopulationFailed = false,
                                    isQuranPopulating = true
                                )
                            }
                            Log.d(TAG, "Quran population progress: $progress")
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            settingsRepository.setQuranPopulated()
                            Log.d(TAG, "Quran population FINISHED")
                            _uiState.update { it.copy(isQuranPopulating = false) }
                            if (_uiState.value.page == OnboardingPage.QURAN_SETUP) {
                                advance()
                            }
                        }

                        WorkInfo.State.FAILED -> {
                            val error = info.outputData.getString("error") 
                                ?: info.progress.getString("error")
                            Log.e(TAG, "Quran population FAILED: $error")
                            _uiState.update {
                                it.copy(
                                    quranPopulationFailed = true,
                                    isQuranPopulating = false
                                )
                            }
                        }

                        WorkInfo.State.CANCELLED -> {
                            _uiState.update { it.copy(isQuranPopulating = false) }
                        }

                        else -> Unit
                    }
                }
        }
    }

    fun refreshPermissionStatuses(context: Context) {
        val locationGranted = ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val exactAlarmGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        _uiState.update {
            it.copy(
                isLocationGranted = locationGranted,
                isNotificationGranted = notificationGranted,
                isExactAlarmGranted = exactAlarmGranted,
                isBatteryOptimizationIgnored = batteryIgnored
            )
        }
    }

    fun onGetStarted() = advance()

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(isLocationGranted = true) }
            fetchLocation()
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "Location denied. You can update this in Settings later."
                )
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(isNotificationGranted = granted) }
    }

    fun onExactAlarmResult(context: Context) {
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            _uiState.update { it.copy(isExactAlarmGranted = alarmManager.canScheduleExactAlarms()) }
        }
    }

    fun onBatteryOptimizationResult(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        _uiState.update { it.copy(isBatteryOptimizationIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)) }
    }

    fun onPermissionsContinue() = advance()

    fun onCalculationMethodChanged(method: CalculationMethod) {
        viewModelScope.launch {
            settingsRepository.setCalculationMethod(method)
            prayerSchedulerManager.enqueueImmediate()
        }
    }

    fun onNotificationSoundChanged(sound: NotificationSound) {
        viewModelScope.launch {
            settingsRepository.setNotificationSound(sound)
        }
    }

    fun onAppThemeChanged(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    fun onSkipQuranSetup() = advance()

    fun onPageSwiped(page: OnboardingPage) {
        _uiState.update { it.copy(page = page) }
    }

    private fun advance() {
        val current = _uiState.value.page
        val nextOrdinal = current.ordinal + 1
        if (nextOrdinal < OnboardingPage.entries.size) {
            val nextPage = OnboardingPage.entries[nextOrdinal]
            _uiState.update { it.copy(page = nextPage) }
        }
    }

    fun onOnboardingComplete() {
        prayerSchedulerManager.enqueueImmediate()
        viewModelScope.launch {
            settingsRepository.setOnboardingDone()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, errorMessage = null) }
            try {
                val latLng = locationFetcher.fetch()
                if (latLng != null) {
                    val saved = locationRepo.saveLocation(latLng.first, latLng.second)
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            savedLocation = saved
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            errorMessage = "Could not determine your location."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLocation = false,
                        errorMessage = "Location error: ${e.message}"
                    )
                }
            }
        }
    }

    fun startLocalDatabasePopulation() {
        workerManager.enqueueUniqueWork(
            QuranPopulationWorker.LOCAL_QURAN_POPULATION_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<QuranPopulationWorker>().build()
        )

        workerManager.enqueueUniqueWork(
            TafsirPopulationWorker.TAFSIR_POPULATION_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<TafsirPopulationWorker>().build()
        )
        advance()

    }

    fun startDatabasePopulation() {
        workerManager.enqueueUniqueWork(
            DATABASE_POPULATION_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<DatabasePopulationWorker>().build()
        )
    }

    fun retryQuranPopulation() {
        _uiState.update { it.copy(quranPopulationFailed = false, populationProgress = 0f) }
        startDatabasePopulation()
    }
}
