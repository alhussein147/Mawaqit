package com.hussein.mawaqit.presentation.onboarding

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.application
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.hussein.mawaqit.MyApp
import com.hussein.mawaqit.data.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.data.infrastructure.location.LocationRepository
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val locationRepo: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val locationFetcher: CurrentLocationFetcher
) : ViewModel() {


    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // ---------------------------------------------------------------------------
    // Events
    // ---------------------------------------------------------------------------

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
    fun onBatteryOptimizationResult() = onOnboardingComplete()
    fun onSkipBatteryOptimization() = onOnboardingComplete()

    /**
     * After notification: go to EXACT_ALARM on Android 12+ (where the permission
     * is needed and might not be granted), otherwise skip straight to BATTERY.
     */
    private fun advanceFromNotification() {
        _uiState.update { it.copy(page = OnboardingPage.EXACT_ALARM) }
    }

    private fun advanceFromExactAlarm() {
        _uiState.update { it.copy(page = OnboardingPage.BATTERY_OPTIMIZATION) }
    }

    private fun onOnboardingComplete() {
        viewModelScope.launch {
            settingsRepository.setOnboardingDone()
            _uiState.update { it.copy(page = OnboardingPage.DONE) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }


    // ---------------------------------------------------------------------------
    // Private
    // ---------------------------------------------------------------------------

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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MyApp)
                val container = application.appContainer
                OnboardingViewModel(
                    locationRepo = container.locationRepository,
                    settingsRepository = container.settingsRepository,
                    locationFetcher = container.locationFetcher
                )
            }
        }
    }

}

