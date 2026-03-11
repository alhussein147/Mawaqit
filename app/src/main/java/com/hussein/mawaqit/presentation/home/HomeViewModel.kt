package com.hussein.mawaqit.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.islamicapp.prayer.PrayerTimeCalculator
import com.hussein.mawaqit.MyApp
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
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import com.example.islamicapp.prayer.Prayer as AppPrayer

class HomeViewModel(val locationRepo: LocationRepository) : ViewModel() {


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

                val hijriDate = toHijriDateString(now)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cityName = location.cityName,
                        rawPrayers = schedule.prayers,
                        hijriDate = hijriDate
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as MyApp)
                val locationRepo = application.appContainer.locationRepository
                HomeViewModel(locationRepo = locationRepo)
            }
        }
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
    val hijriDate: String = ""

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

@OptIn(ExperimentalTime::class)
private fun toHijriDateString(instant: Instant): String {
    val ld = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
    val y = ld.year
    val m = ld.month.number
    val d = ld.day

    // Julian Day Number from Gregorian
    val jdn = (1461 * (y + 4800 + (m - 14) / 12)) / 4 +
            (367 * (m - 2 - 12 * ((m - 14) / 12))) / 12 -
            (3 * ((y + 4900 + (m - 14) / 12) / 100)) / 4 + d - 32075

    // Hijri from JDN
    var l = jdn - 1948440 + 10632

    val n = (l - 1) / 10631
    l = l - 10631 * n + 354
    val j = ((10985 - l) / 5316) * ((50 * l) / 17719) +
            (l / 5670) * ((43 * l) / 15238)
    l = l - ((30 - j) / 15) * ((17719 * j) / 50) -
            (j / 16) * ((15238 * j) / 43) + 29
    val hYear = 30 * n + j - 30
    val hMonth = (24 * l) / 709
    val hDay = l - (709 * hMonth) / 24

    val monthNames = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
        "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    val monthName = monthNames.getOrElse(hMonth - 1) { "" }
    return "$hDay $monthName $hYear"
}

enum class PrayerStatus { PASSED, CURRENT, UPCOMING }