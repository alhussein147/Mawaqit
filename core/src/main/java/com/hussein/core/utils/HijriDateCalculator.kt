package com.hussein.core.utils

import com.batoulapps.adhan2.CalculationMethod
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriMonth(
    val englishName: String,
    val arabicName: String
)

object HijriDateCalculator {

    val hijriMonths = listOf(
        HijriMonth("Muharram", "محرم"),
        HijriMonth("Safar", "صفر"),
        HijriMonth("Rabi' al-Awwal", "ربيع الأول"),
        HijriMonth("Rabi' al-Thani", "ربيع الآخر"),
        HijriMonth("Jumada al-Awwal", "جمادى الأولى"),
        HijriMonth("Jumada al-Thani", "جمادى الآخرة"),
        HijriMonth("Rajab", "رجب"),
        HijriMonth("Sha'ban", "شعبان"),
        HijriMonth("Ramadan", "رمضان"),
        HijriMonth("Shawwal", "شوال"),
        HijriMonth("Dhu al-Qi'dah", "ذو القعدة"),
        HijriMonth("Dhu al-Hijjah", "ذو الحجة")
    )

    /**
     * Converts a [kotlinx.datetime.Instant] to a formatted Hijri date string.
     * Uses the Umm al-Qura calendar (default for java.time.chrono.HijrahDate).
     */
    fun toHijriDateString(
        instant: kotlin.time.Instant,
        calculationMethod: CalculationMethod = CalculationMethod.EGYPTIAN
    ): String {
        val javaInstant = java.time.Instant.ofEpochSecond(
            instant.epochSeconds,
            instant.nanosecondsOfSecond.toLong()
        )
        val daysToSubtract = when(calculationMethod){
            CalculationMethod.UMM_AL_QURA -> 1
            else -> 0
        }
        val localDate = javaInstant.atZone(ZoneId.systemDefault()).toLocalDate()

        val hijrahDate = HijrahDate.from(localDate)

        val hYear = hijrahDate.get(ChronoField.YEAR)
        val hMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
        val hDay = hijrahDate.get(ChronoField.DAY_OF_MONTH) - daysToSubtract

        val monthName = hijriMonths.getOrNull(hMonth - 1)?.englishName ?: ""

        return "$hDay $monthName $hYear"
    }

    /**
     * Converts an integer to its Arabic digit representation.
     */
    fun Int.toArabicDigits(): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        return this.toString().map { char ->
            if (char.isDigit()) arabicDigits[char - '0'] else char
        }.joinToString("")
    }
}
