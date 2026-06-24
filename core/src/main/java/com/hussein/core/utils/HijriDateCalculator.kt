package com.hussein.core.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

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

    @OptIn(ExperimentalTime::class)
     fun toHijriDateString(instant: Instant): String {
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
        val hYear = (30 * n + j - 30)
        val hMonth = (24 * l) / 709
        val hDay = (l - (709 * hMonth) / 24)

        val monthName = hijriMonths.getOrNull(hMonth - 1)?.englishName ?: ""

        return "$hDay $monthName $hYear"
    }

    fun Int.toArabicDigits(): String {
        val englishNumbers = listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
        val arabicNumbers = listOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")

        var numString = this.toString()
        for (i in 0..9) {
            numString = numString.replace(englishNumbers[i], arabicNumbers[i])
        }
        return numString
    }

}