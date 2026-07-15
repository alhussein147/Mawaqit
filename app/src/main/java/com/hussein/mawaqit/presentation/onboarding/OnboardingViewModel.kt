package com.hussein.mawaqit.presentation.onboarding

import android.app.AlarmManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.LocationRepository
import com.hussein.core.models.SavedLocation
import com.hussein.mawaqit.infrastructure.connectivity.NetworkObserver
import com.hussein.mawaqit.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.infrastructure.settings.AppSettings
import com.hussein.mawaqit.infrastructure.settings.AppTheme
import com.hussein.mawaqit.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.infrastructure.workers.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.domain.location.RefreshLocationUseCase
import com.hussein.mawaqit.presentation.onboarding.components.OnboardingPage
import com.hussein.mawaqit.presentation.onboarding.components.PermissionState
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
    val locationPermissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val notificationPermissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val exactAlarmPermissionState: PermissionState = PermissionState.NOT_REQUESTED,
    val batteryOptimizationState: PermissionState = PermissionState.NOT_REQUESTED,
    val isOffline: Boolean = false,
    val settings: AppSettings? = null
)

class OnboardingViewModel(
    private val locationRepo: LocationRepository,
    private val settingsRepository: SettingsRepository,
    private val locationFetcher: CurrentLocationFetcher,
    private val prayerSchedulerManager: PrayerSchedulerManager,
    private val networkObserver: NetworkObserver,
    private val refreshLocationUseCase: RefreshLocationUseCase
) : ViewModel() {

    private val TAG = "OnboardingViewModel"
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
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
            val alarmManager =
                context.getSystemService(AlarmManager::class.java)
            alarmManager.canScheduleExactAlarms()
        } else true

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)

        _uiState.update { state ->
            state.copy(
                locationPermissionState = if (locationGranted) PermissionState.GRANTED
                else if (state.locationPermissionState == PermissionState.PERMANENTLY_DENIED) PermissionState.PERMANENTLY_DENIED
                else if (state.locationPermissionState == PermissionState.DENIED) PermissionState.DENIED
                else PermissionState.NOT_REQUESTED,

                notificationPermissionState = if (notificationGranted) PermissionState.GRANTED
                else if (state.notificationPermissionState == PermissionState.PERMANENTLY_DENIED) PermissionState.PERMANENTLY_DENIED
                else if (state.notificationPermissionState == PermissionState.DENIED) PermissionState.DENIED
                else PermissionState.NOT_REQUESTED,

                exactAlarmPermissionState = if (exactAlarmGranted) PermissionState.GRANTED
                else if (state.exactAlarmPermissionState == PermissionState.PERMANENTLY_DENIED) PermissionState.PERMANENTLY_DENIED
                else if (state.exactAlarmPermissionState == PermissionState.DENIED) PermissionState.DENIED
                else PermissionState.NOT_REQUESTED,

                batteryOptimizationState = if (batteryIgnored) PermissionState.GRANTED
                else if (state.batteryOptimizationState == PermissionState.PERMANENTLY_DENIED) PermissionState.PERMANENTLY_DENIED
                else if (state.batteryOptimizationState == PermissionState.DENIED) PermissionState.DENIED
                else PermissionState.NOT_REQUESTED
            )
        }
    }

    fun onGetStarted() = advance()

    fun onLocationPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        if (granted) {
            _uiState.update { it.copy(locationPermissionState = PermissionState.GRANTED) }
            fetchLocation()
        } else {
            _uiState.update {
                it.copy(
                    locationPermissionState = if (permanentlyDenied) PermissionState.PERMANENTLY_DENIED else PermissionState.DENIED,
                    errorMessage = if (permanentlyDenied) "Location permanently denied. Please enable it in Settings."
                    else "Location denied. You can update this in Settings later."
                )
            }
        }
    }

    fun onNotificationPermissionResult(granted: Boolean, permanentlyDenied: Boolean = false) {
        _uiState.update { 
            it.copy(
                notificationPermissionState = when {
                    granted -> PermissionState.GRANTED
                    permanentlyDenied -> PermissionState.PERMANENTLY_DENIED
                    else -> PermissionState.DENIED
                }
            ) 
        }
    }

    fun onExactAlarmResult(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager =
                context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val isGranted = alarmManager.canScheduleExactAlarms()
            _uiState.update { 
                it.copy(exactAlarmPermissionState = if (isGranted) PermissionState.GRANTED else PermissionState.DENIED) 
            }
        }
    }

    fun onBatteryOptimizationResult(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnored = powerManager.isIgnoringBatteryOptimizations(context.packageName)
        _uiState.update {
            it.copy(
                batteryOptimizationState = if (isIgnored) PermissionState.GRANTED else PermissionState.DENIED
            )
        }
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
        if (page == OnboardingPage.DONE) {
            checkAndFetchLocationIfMissing()
        }
    }

    private fun advance() {
        val current = _uiState.value.page
        val nextOrdinal = current.ordinal + 1
        if (nextOrdinal < OnboardingPage.entries.size) {
            val nextPage = OnboardingPage.entries[nextOrdinal]
            _uiState.update { it.copy(page = nextPage) }
            if (nextPage == OnboardingPage.DONE) {
                checkAndFetchLocationIfMissing()
            }
        }
    }

    private fun checkAndFetchLocationIfMissing() {
        viewModelScope.launch {
            val hasLocation = locationRepo.hasLocation()
            if (!hasLocation && !_uiState.value.isOffline) {
                Log.d(TAG, "Location missing on Done page, attempting auto-fetch via IP...")
                fetchLocation()
            }
        }
    }

    fun onOnboardingComplete() {
        prayerSchedulerManager.enqueueImmediate()
        viewModelScope.launch {
            settingsRepository.setOnboardingDone()
        }
    }


    private fun fetchLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLocation = true, errorMessage = null) }
            val result = refreshLocationUseCase.execute(fallbackToIp = true)
            _uiState.update {
                result.fold(
                    onSuccess = { location ->
                        it.copy(
                            isLoadingLocation = false,
                            savedLocation = location
                        )
                    },
                    onFailure = { error ->
                        it.copy(
                            isLoadingLocation = false,
                            errorMessage = error.message
                        )
                    }
                )
            }
        }
    }

}
