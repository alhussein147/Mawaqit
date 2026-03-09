package com.hussein.mawaqit.data.infrastructure.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.islamicapp.prayer.PrayerTimeCalculator
import com.hussein.mawaqit.data.infrastructure.alarm_manager.PrayerAlarmManager
import com.hussein.mawaqit.data.infrastructure.location.LocationRepository
import com.hussein.mawaqit.data.infrastructure.notification.NotificationUtils
import com.hussein.mawaqit.data.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager
import kotlinx.coroutines.flow.first
import kotlin.time.ExperimentalTime

/**
 * Runs once per day (just after midnight) to:
 *  1. Read stored user coordinates
 *  2. Read current settings (calculation method + per-prayer notification toggles)
 *  3. Calculate today's prayer times
 *  4. Cancel old alarms
 *  5. Schedule alarms only for prayers that have notifications enabled
 */
class DailyPrayerWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "DailyPrayerWorker"
        const val WORK_NAME = "daily_prayer_scheduler"
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun doWork(): Result {
        return try {
            val locationRepo = LocationRepository(applicationContext)
            val settingsRepo = SettingsRepository(applicationContext)

            val location = locationRepo.getSavedLocation()

            if (location == null) {
                Log.w(TAG, "Location not set — skipping prayer scheduling")
                return Result.failure()
            }


            // Both notification channels are blocked by the user —
            // nothing to schedule, but this is a valid user preference not a failure
            if (!NotificationUtils.areChannelEnabled(applicationContext)) return Result.success()

            val alarmManager = PrayerAlarmManager(applicationContext)

            // Read the current settings snapshot (first emission from Flow)
            val settings = settingsRepo.settingsFlow.first()

            val notificationSoundPref = settings.notificationSound

            // checking if the user disabled prayer notifications from the app, to avoid scheduling alarms
            if (notificationSoundPref == NotificationSound.NONE) {
                alarmManager.cancelAll()
                return Result.success()
            }

            // Calculate today's prayer times using the user's chosen method
            val schedule = PrayerTimeCalculator.calculate(
                latitude = location.latitude,
                longitude = location.longitude,
                method = settings.calculationMethod
            )

            alarmManager.cancelAll()

            // Only schedule alarms for prayers that have notifications enabled
            val filteredSchedule = schedule.copy(
                prayers = schedule.prayers.filter { prayer ->
                    settingsRepo.isNotificationEnabled(prayer.name, settings)
                }
            )

            alarmManager.scheduleAll(filteredSchedule)

            Log.d(TAG, "Scheduled ${filteredSchedule.prayers.size} / 5 prayers")

            // scheduling a work for scheduling prayer alarms tomorrow
            PrayerSchedulerManager.enqueueTomorrow(applicationContext)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "DailyPrayerWorker failed: ${e.message}", e)
            Result.retry()
        }
    }


}