package com.hussein.mawaqit.presentation.settings


import CurrentLocationFetcher
import android.R.attr.theme
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.LocationRepository
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.MyApp

import com.hussein.mawaqit.data.infrastructure.settings.AppColorScheme
import com.hussein.mawaqit.data.infrastructure.settings.AppSettings
import com.hussein.mawaqit.data.infrastructure.settings.AppTheme
import com.hussein.mawaqit.data.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.presentation.home.HomeViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    val TAG = "SettingsViewModel"
    private val repo = SettingsRepository(application)
    private val locationRepo = LocationRepository(application)
    private val currentLocationFetcher = CurrentLocationFetcher(application)



    private val _locationState = MutableStateFlow<LocationUpdateState>(LocationUpdateState.Idle)
    val locationState: StateFlow<LocationUpdateState> = _locationState.asStateFlow()

    private val _savedLocation = MutableStateFlow<SavedLocation?>(null)
    val savedLocation: StateFlow<SavedLocation?> = _savedLocation.asStateFlow()

    init {
        viewModelScope.launch {
            _savedLocation.value = locationRepo.getSavedLocation()
        }
    }

    val settings: StateFlow<AppSettings?> = repo.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )


    fun onPrayerNotificationToggled(prayerName: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.setPrayerNotificationEnabled(prayerName, enabled)
            // Reschedule alarms so the change takes effect immediately
            PrayerSchedulerManager.enqueueImmediate(getApplication())
        }
    }

    fun onCalculationMethodChanged(method: CalculationMethod) {
        viewModelScope.launch {
            repo.setCalculationMethod(method)
            // Prayer times change with the method — reschedule
            PrayerSchedulerManager.enqueueImmediate(getApplication())
        }
    }

    fun onNotificationSoundChanged(sound: NotificationSound) {
        viewModelScope.launch {
            repo.setNotificationSound(sound)
        }
    }


    fun onAppThemeChanged(theme: AppTheme) {
        viewModelScope.launch {
            repo.setAppTheme(theme)
        }
    }

    fun onAppColorSchemeChanged(colorScheme: AppColorScheme) {
        viewModelScope.launch {
            repo.setAppColorScheme(colorScheme)
        }
    }

    // location updating
    // ---------------------------------------------------------------------------
    // Location update
    // ---------------------------------------------------------------------------

    /**
     * Called by the screen once location permission is confirmed granted.
     * Fetches the current position and persists it.
     */
    fun fetchAndSaveLocation() {
        viewModelScope.launch {
            _locationState.update { LocationUpdateState.Fetching }
            try {
                val latLng = currentLocationFetcher.fetch()
                if (latLng != null) {
                    val saved = locationRepo.saveLocation(latLng.first, latLng.second)
                    // Reschedule prayer alarms for the new location
                    PrayerSchedulerManager.enqueueImmediate(getApplication())
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MyApp)
                SettingsViewModel(application = application)
            }
        }
    }

    sealed interface LocationUpdateState {
        data object Idle : LocationUpdateState
        data object Fetching : LocationUpdateState
        data class Success(val location: SavedLocation) : LocationUpdateState
        data class Error(val message: String) : LocationUpdateState
    }
}