package com.hussein.mawaqit.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.islamicapp.prayer.PrayerTimeCalculator
import com.hussein.mawaqit.data.infrastructure.location.LocationRepository
import com.hussein.mawaqit.data.infrastructure.location.SavedLocation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.example.islamicapp.prayer.Prayer as AppPrayer

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val locationRepo = LocationRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _countdown = MutableStateFlow<CountdownTime?>(null)
    val countdown: StateFlow<CountdownTime?> = _countdown.asStateFlow()

    private var tickerJob: Job? = null
    private var lastKnownDate: LocalDate? = null

    private val reloadTrigger = MutableSharedFlow<Unit>(replay = 1).also {
        it.tryEmit(Unit) // seed so combine emits immediately on start
    }

    init {
        // Re-calculate prayers whenever the saved location changes.
        // This means navigating back from com.hussein.islamic.presentation.Settings after a location update
        // automatically shows the correct prayer times — no manual refresh needed.
        viewModelScope.launch {
            locationRepo.locationFlow.collectLatest { location ->
                reloadTrigger.collect {
                    loadPrayers(location)
                }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Load
    // ---------------------------------------------------------------------------

    @OptIn(ExperimentalTime::class)
    fun loadPrayers(location: SavedLocation?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (location == null) {
                _uiState.update { it.copy(isLoading = false, error = "Location not set.") }
                return@launch
            }

            try {
                val now = Clock.System.now()
                val schedule =
                    PrayerTimeCalculator.calculate(location.latitude, location.longitude, now)
                lastKnownDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cityName = location.cityName,
                        rawPrayers = schedule.prayers
                    )
                }
                tick()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to calculate prayer times."
                    )
                }
            }
        }
    }


    @OptIn(ExperimentalTime::class)
    fun tick() {
        val now = Clock.System.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

        if (lastKnownDate != null && today != lastKnownDate) {
            lastKnownDate = today
            viewModelScope.launch { reloadTrigger.emit(Unit) }
            return // loadPrayers will re-classify once the new schedule is ready
        }

        val rawPrayers = _uiState.value.rawPrayers
        if (rawPrayers.isEmpty()) return

        val classified = classifyPrayers(rawPrayers, now)
        val next = classified.firstOrNull { it.status == PrayerStatus.UPCOMING }
            ?: classified.firstOrNull { it.status == PrayerStatus.CURRENT }

        if (classified != _uiState.value.prayers || next != _uiState.value.nextPrayer) {
            _uiState.update { it.copy(prayers = classified, nextPrayer = next) }
        }

        _countdown.value = next
            ?.takeIf { it.status == PrayerStatus.UPCOMING }
            ?.let { buildCountdown(it.time, now) }
    }



    /**
     * PASSED   — prayer time has elapsed and it is not the current window
     * CURRENT  — the most recent prayer whose time has passed (active window)
     * UPCOMING — prayer time has not been reached yet
     */
    @OptIn(ExperimentalTime::class)
    private fun classifyPrayers(
        prayers: List<AppPrayer>,
        now: Instant
    ): List<PrayerUiModel> {
        val currentIndex = prayers.indexOfLast { it.time <= now }
        return prayers.mapIndexed { index, prayer ->
            val status = when {
                index < currentIndex -> PrayerStatus.PASSED
                index == currentIndex -> PrayerStatus.CURRENT
                else -> PrayerStatus.UPCOMING
            }
            PrayerUiModel(name = prayer.name, time = prayer.time, status = status)
        }
    }

    // ---------------------------------------------------------------------------
    // Countdown
    // ---------------------------------------------------------------------------

    @OptIn(ExperimentalTime::class)
    private fun buildCountdown(target: Instant, now: Instant): CountdownTime {
        val diff = (target - now).coerceAtLeast(Duration.ZERO)
        val totalSeconds = diff.inWholeSeconds
        return CountdownTime(
            hours = totalSeconds / 3600,
            minutes = (totalSeconds % 3600) / 60
        )
    }


    override fun onCleared() {
        super.onCleared()
        tickerJob?.cancel()
    }
}

// ---------------------------------------------------------------------------
// State & models
// ---------------------------------------------------------------------------

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cityName: String = "",
    val rawPrayers: List<AppPrayer> = emptyList(),
    val prayers: List<PrayerUiModel> = emptyList(),
    val nextPrayer: PrayerUiModel? = null,
)

data class PrayerUiModel @OptIn(ExperimentalTime::class) constructor(
    val name: String,
    val time: Instant,
    val status: PrayerStatus
)

data class CountdownTime(val hours: Long, val minutes: Long) {
    override fun toString(): String =
        if (hours > 0) "%d H %02d Min".format(hours, minutes)
        else "%d Min".format(minutes)
}

enum class PrayerStatus { PASSED, CURRENT, UPCOMING }