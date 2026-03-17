package com.hussein.mawaqit.data.infrastructure.alarm_manager

import kotlin.jvm.java

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hussein.core.models.Prayer
import com.hussein.core.models.PrayerSchedule
import com.hussein.mawaqit.data.infrastructure.receivers.PrayerAlarmReceiver
import kotlin.time.ExperimentalTime

class PrayerAlarmManager(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "PrayerAlarmManager"
        private val PRAYER_REQUEST_CODES = mapOf(
            "Fajr"    to 1001,
            "Dhuhr"   to 1002,
            "Asr"     to 1003,
            "Maghrib" to 1004,
            "Isha"    to 1005
        )
    }

    @OptIn(ExperimentalTime::class)
    fun scheduleAll(schedule: PrayerSchedule) {
        val nowMs = System.currentTimeMillis()
        schedule.prayers.forEach { prayer ->
            if (prayer.time.toEpochMilliseconds() > nowMs) scheduleSingle(prayer)
            else Log.d(TAG, "${prayer.name} already passed today, skipping.")
        }
    }

    @OptIn(ExperimentalTime::class)
    fun scheduleSingle(prayer: Prayer) {
        val requestCode = PRAYER_REQUEST_CODES[prayer.name] ?: return
        val pendingIntent = buildPendingIntent(prayer.name, requestCode)
        val triggerAtMillis = prayer.time.toEpochMilliseconds()

        // On Android 12+, SCHEDULE_EXACT_ALARM permission is required.
        // If it's been revoked, skip silently — DailyPrayerWorker checks
        // canScheduleExactAlarms() before calling us, so this is a safety net only.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission not granted — skipping \${prayer.name}")
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
        )
        Log.d(TAG, "Scheduled \${prayer.name} at \${prayer.time}")
    }

    fun cancelAll() {
        PRAYER_REQUEST_CODES.forEach { (name, code) ->
            alarmManager.cancel(buildPendingIntent(name, code))
        }
    }

    fun canScheduleExactAlarms(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms()
        else true

    private fun buildPendingIntent(prayerName: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_TIME
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}