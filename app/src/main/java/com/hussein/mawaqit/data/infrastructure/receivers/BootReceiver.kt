package com.hussein.mawaqit.data.infrastructure.receivers

import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Listens for device boot completion and reschedules all prayer alarms.
 *
 * AlarmManager alarms are wiped on reboot, so this receiver re-triggers
 * [DailyPrayerWorker] immediately after the device restarts.
 *
 * Register in AndroidManifest.xml:
 *
 *   <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
 *
 *   <receiver android:name=".prayer.BootReceiver"
 *             android:exported="true">
 *       <intent-filter>
 *           <action android:name="android.intent.action.BOOT_COMPLETED"/>
 *           <action android:name="android.intent.action.MY_PACKAGE_REPLACED"/>
 *       </intent-filter>
 *   </receiver>
 *
 * MY_PACKAGE_REPLACED handles app updates, which also clear alarms.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.d(TAG, "Boot/update detected — rescheduling prayer alarms")
                PrayerSchedulerManager.enqueueImmediate(context)
            }
        }
    }
}