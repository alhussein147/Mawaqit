package com.hussein.mawaqit.presentation.settings


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.LocationRepository
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.infrastructure.alarm_manager.PrayerAlarmManager
import com.hussein.mawaqit.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.infrastructure.settings.AppColorScheme
import com.hussein.mawaqit.infrastructure.settings.AppSettings
import com.hussein.mawaqit.infrastructure.settings.AppTheme
import com.hussein.mawaqit.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.infrastructure.workers.prayer.PrayerSchedulerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val locationRepository: LocationRepository,
    private val currentLocationFetcher: CurrentLocationFetcher,
    private val prayerSchedulerManager: PrayerSchedulerManager,
    private val prayerAlarmManager: PrayerAlarmManager
) : ViewModel() {
    val TAG = "SettingsViewModel"

    private val _locationState = MutableStateFlow<LocationUpdateState>(LocationUpdateState.Idle)
    val locationState: StateFlow<LocationUpdateState> = _locationState.asStateFlow()

    private val _savedLocation = MutableStateFlow<SavedLocation?>(null)
    val savedLocation: StateFlow<SavedLocation?> = _savedLocation.asStateFlow()

    init {
        viewModelScope.launch {
            _savedLocation.value = locationRepository.getSavedLocation()
        }
    }

    val settings: StateFlow<AppSettings?> = settingsRepository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )


    fun onPrayerNotificationToggled(prayerName: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setPrayerNotificationEnabled(prayerName, enabled)
        }
    }

    fun onCalculationMethodChanged(method: CalculationMethod) {
        viewModelScope.launch {
            settingsRepository.setCalculationMethod(method)
            // Prayer times change with the method — reschedule
            prayerSchedulerManager.enqueueImmediate()
        }
    }

    fun onNotificationStyleChanged(sound: NotificationSound) {
        viewModelScope.launch {
            settingsRepository.setNotificationSound(sound)
            if (sound == NotificationSound.NONE) {
                prayerSchedulerManager.cancel()
                prayerAlarmManager.cancelAll()
            } else {
                // Prayer times notification is enabled — reschedule
                prayerSchedulerManager.enqueueImmediate()
            }
        }
    }


    fun onAppThemeChanged(theme: AppTheme) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    fun onAppColorSchemeChanged(colorScheme: AppColorScheme) {
        viewModelScope.launch {
            settingsRepository.setAppColorScheme(colorScheme)
        }
    }

    fun fetchAndSaveLocation() {
        viewModelScope.launch {
            _locationState.update { LocationUpdateState.Fetching }
            try {
                val userLocation = currentLocationFetcher.fetch(fallbackToIp = false)
                if (userLocation != null) {
                    val saved = locationRepository.saveLocation(
                        latitude = userLocation.latitude,
                        longitude = userLocation.longitude,
                        cityName = userLocation.city
                    )
                    // Reschedule prayer alarms for the new location
                    prayerSchedulerManager.enqueueImmediate()
                    _savedLocation.value = saved
                    _locationState.update { LocationUpdateState.Success(saved) }
                    Log.d(TAG, "fetchAndSaveLocation- newLocation: $saved ")
                } else {
                    _locationState.update { LocationUpdateState.Error("Could not determine location. Make sure GPS is enabled.") }
                }
            } catch (e: Exception) {
                _locationState.update { LocationUpdateState.Error("Location error: ${e.message}") }
            }
        }
    }

    fun resetLocationState() {
        _locationState.update { LocationUpdateState.Idle }
    }

    sealed interface LocationUpdateState {
        data object Idle : LocationUpdateState
        data object Fetching : LocationUpdateState
        data class Success(val location: SavedLocation) : LocationUpdateState
        data class Error(val message: String) : LocationUpdateState
    }
}