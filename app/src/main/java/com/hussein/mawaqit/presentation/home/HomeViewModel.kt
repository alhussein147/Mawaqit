package com.hussein.mawaqit.presentation.home

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.batoulapps.adhan2.CalculationMethod
import com.hussein.core.LocationRepository
import com.hussein.core.PrayerTimeCalculator
import com.hussein.core.models.SavedLocation
import com.hussein.core.utils.HijriDateCalculator
import com.hussein.mawaqit.MawaqitApp
import com.hussein.mawaqit.data.db.AyahDao
import com.hussein.mawaqit.data.db.AyahEntity
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.models.Ayah
import com.hussein.mawaqit.data.db.models.AyahOfTheDay
import com.hussein.mawaqit.data.db.models.AyahWithSurah
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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
import com.hussein.core.models.Prayer as AppPrayer

@Stable
data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cityName: String = "",
    val rawPrayers: List<AppPrayer> = emptyList(),
    val prayers: List<PrayerUiModel> = emptyList(),
    val nextPrayer: PrayerUiModel? = null,
    val hijriDate: String = "",
    val ayahOfTheDay: AyahOfTheDay? = null

)
@Stable
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



@OptIn(ExperimentalTime::class)
class HomeViewModel(
    val locationRepo: LocationRepository,
    val settingsRepository: SettingsRepository,
    val quranDatabaseRepository: QuranDatabaseRepository
) : ViewModel() {


    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val _countdown = MutableStateFlow<CountdownTime?>(null)
    val countdown: StateFlow<CountdownTime?> = _countdown.asStateFlow()
    private var tickerJob: Job? = null
    private var lastKnownDate: LocalDate? = null

    private val reloadTrigger = MutableSharedFlow<Unit>(replay = 1).also {
        it.tryEmit(Unit) // seed so combine emits immediately on start
    }

    init {
        viewModelScope.launch {
            // Combine all sources: location, settings, and manual/daily reloads
            combine(
                locationRepo.locationFlow,
                settingsRepository.settingsFlow,
                reloadTrigger
            ) { location, settings, _ ->
                // Extract the data needed for loadPrayers
                location to settings.calculationMethod
            }.collectLatest { (location, method) ->
                loadPrayers(location, method)
            }
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(ayahOfTheDay = quranDatabaseRepository.getAyahOfTheDay())
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun loadPrayers(location: SavedLocation?, calculationMethod: CalculationMethod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (location == null) {
                _uiState.update { it.copy(isLoading = false, error = "Location not set.") }
                return@launch
            }

            try {
                val now = Clock.System.now()
                val schedule =
                    PrayerTimeCalculator.calculate(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        instant = now,
                        method = calculationMethod
                    )
                lastKnownDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date

                val hijriDate = HijriDateCalculator.toHijriDateString(now)

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

