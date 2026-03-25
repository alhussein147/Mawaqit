package com.hussein.mawaqit.presentation.onboarding


import CurrentLocationFetcher
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hussein.core.LocationRepository
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.infrastructure.workers.QuranPopulationWorker
import com.hussein.mawaqit.data.infrastructure.workers.QuranPopulationWorker.Companion.WORK_NAME
import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingPage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val page: OnboardingPage = OnboardingPage.WELCOME,
    val isLoadingLocation: Boolean = false,
    val notificationGranted: Boolean = false,
    val savedLocation: SavedLocation? = null,
    val errorMessage: String? = null,
    val quranProgress: Float = 0f,
    val quranCurrentSurah: Int = 0,
    val quranPopulationFailed: Boolean = false
)

class OnboardingViewModel(
    private val locationRepo: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val locationFetcher: CurrentLocationFetcher,
    private val workerManager: WorkManager,
    private val prayerSchedulerManager: PrayerSchedulerManager
) : ViewModel() {


    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onGetStarted() {
        _uiState.update { it.copy(page = OnboardingPage.LOCATION) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            _uiState.update { it.copy(page = OnboardingPage.FETCHING_LOCATION) }
            fetchLocation()
        } else {
            _uiState.update {
                it.copy(
                    page = OnboardingPage.NOTIFICATION,
                    errorMessage = "Location denied. You can update this in com.hussein.islamic.presentation.Settings later."
                )
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(notificationGranted = granted) }
        advanceFromNotification()
    }

    fun onSkipLocation() {
        _uiState.update { it.copy(page = OnboardingPage.NOTIFICATION, errorMessage = null) }
    }

    fun onSkipNotification() = advanceFromNotification()

    fun onExactAlarmResult() = advanceFromExactAlarm()
    fun onSkipExactAlarm() = advanceFromExactAlarm()

    // Battery optimization is optional — skipping or granting both complete onboarding
    fun onBatteryOptimizationResult() = advanceFromExactAlarm()
    fun onSkipBatteryOptimization() = advanceFromExactAlarm()
    fun onSkipQuranSetup() = advanceFromQuranSetup()

    /**
     * After notification: go to EXACT_ALARM on Android 12+ (where the permission
     * is needed and might not be granted), otherwise skip straight to BATTERY.
     */
    private fun advanceFromNotification() {
        _uiState.update { it.copy(page = OnboardingPage.EXACT_ALARM) }
    }

    private fun advanceFromQuranSetup() {
        _uiState.update { it.copy(page = OnboardingPage.DONE) }
    }

    private fun advanceFromExactAlarm() {
        _uiState.update { it.copy(page = OnboardingPage.QURAN_SETUP) }
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
                            savedLocation = saved,
                            page = OnboardingPage.NOTIFICATION
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoadingLocation = false,
                            errorMessage = "Could not determine your location. Please try again.",
                            page = OnboardingPage.LOCATION
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingLocation = false,
                        errorMessage = "Location error: ${e.message}",
                        page = OnboardingPage.LOCATION
                    )
                }
            }
        }
    }

// TODO : HANDLE WHEN USER HAVE ALREADY GIVEN SOME PERMISSIONS BUT DIDN'T FINISH ONBOARDING

    fun startQuranPopulation() {

        workerManager.enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<QuranPopulationWorker>().build()
        )

        viewModelScope.launch {
                workerManager.getWorkInfosForUniqueWorkFlow(QuranPopulationWorker.WORK_NAME)
                .collect { infos ->
                    val info = infos.firstOrNull() ?: return@collect
                    when (info.state) {
                        WorkInfo.State.RUNNING -> {
                            val progress = info.progress
                                .getFloat(QuranPopulationWorker.KEY_PROGRESS, 0f)
                            val surah = info.progress
                                .getInt(QuranPopulationWorker.KEY_CURRENT_SURAH, 0)
                            _uiState.update {
                                it.copy(
                                    quranProgress = progress,
                                    quranCurrentSurah = surah
                                )
                            }
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            viewModelScope.launch {
                                settingsRepository.setQuranPopulated()
                                advanceFromQuranSetup()
                            }
                        }

                        WorkInfo.State.FAILED -> {
                            _uiState.update { it.copy(quranPopulationFailed = true) }
                        }

                        else -> Unit
                    }
                }
        }
    }

    fun retryQuranPopulation() {
        _uiState.update { it.copy(quranPopulationFailed = false, quranProgress = 0f) }
        startQuranPopulation()
    }
}

