package com.hussein.core

import com.batoulapps.adhan2.CalculationMethod
import com.batoulapps.adhan2.Coordinates
import com.batoulapps.adhan2.PrayerTimes
import com.batoulapps.adhan2.data.DateComponents
import com.hussein.core.models.Prayer
import com.hussein.core.models.PrayerSchedule
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

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

