package com.example.islamicapp.prayer

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Wraps the adhan2 (Kotlin multiplatform) library to calculate prayer times.
 *
 * adhan2 returns [Instant] fields directly — no java.util.Date anywhere.
 *
 * Gradle dep: implementation("com.batoulapps.adhan:adhan2:0.0.6")
 */
object PrayerTimeCalculator {

    @OptIn(ExperimentalTime::class)
    fun calculate(
        latitude: Double,
        longitude: Double,
        instant: kotlin.time.Instant = kotlin.time.Clock.System.now(),
        method: CalculationMethod = CalculationMethod.MUSLIM_WORLD_LEAGUE
    ): PrayerSchedule {
        val coordinates = Coordinates(latitude, longitude)
        val localDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dateComponents = DateComponents(
            localDate.year,
            localDate.month.number,
            localDate.day
        )
        val prayerTimes = PrayerTimes(coordinates, dateComponents, method.parameters)

        return PrayerSchedule(
            prayers = listOf(
                Prayer("Fajr", prayerTimes.fajr),
                Prayer("Dhuhr", prayerTimes.dhuhr),
                Prayer("Asr", prayerTimes.asr),
                Prayer("Maghrib", prayerTimes.maghrib),
                Prayer("Isha", prayerTimes.isha)
            )
        )
    }
}

data class Prayer @OptIn(ExperimentalTime::class) constructor(
    val name: String,
    val time: kotlin.time.Instant
)

data class PrayerSchedule(val prayers: List<Prayer>)