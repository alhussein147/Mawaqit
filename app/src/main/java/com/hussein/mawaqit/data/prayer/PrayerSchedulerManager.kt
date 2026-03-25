package com.hussein.mawaqit.data.prayer


import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hussein.mawaqit.data.infrastructure.workers.DailyPrayerWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Manages WorkManager enqueueing for [DailyPrayerWorker].
 *
 * Uses one-time work (not periodic) so we can target exactly 1 minute past midnight
 * each day, avoiding drift that PeriodicWorkRequest's inexact timing can introduce.
 */
class PrayerSchedulerManager(val workManager: WorkManager) {

    private  val TAG = "PrayerSchedulerManager"

    /**
     * Call this on app launch and after BOOT_COMPLETED.
     * Runs the worker immediately if no work is pending,
     * which handles first-run and post-reboot cases.
     */
    fun enqueueImmediate() {
        val request = OneTimeWorkRequestBuilder<DailyPrayerWorker>()
            .build()

        workManager.enqueueUniqueWork(
                DailyPrayerWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,   // Don't replace if already scheduled
                request
            )

        Log.d(TAG, "Enqueued immediate DailyPrayerWorker")
    }

    /**
     * Schedules the worker to fire at 00:01 tomorrow (1 minute past midnight).
     * Called at the end of each [DailyPrayerWorker] run to keep the daily chain going.
     */
    fun enqueueTomorrow() {
        val delayMillis = millisUntilTomorrow()
        Log.d(TAG, "Next DailyPrayerWorker in ${delayMillis / 1000 / 60} minutes")

        val request = OneTimeWorkRequestBuilder<DailyPrayerWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()

        workManager
            .enqueueUniqueWork(
                DailyPrayerWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,  // Replace any previous pending run
                request
            )
    }

    /**
     * Cancels any pending daily worker (e.g. when the user disables notifications).
     */
    fun cancel(context: Context) {
        WorkManager.getInstance(context)
            .cancelUniqueWork(DailyPrayerWorker.WORK_NAME)
        Log.d(TAG, "Cancelled DailyPrayerWorker")
    }

    /**
     * Returns the milliseconds from now until 00:01 tomorrow.
     */
    private fun millisUntilTomorrow(): Long {
        val now = Calendar.getInstance()
        val tomorrow = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return tomorrow.timeInMillis - now.timeInMillis
    }
}