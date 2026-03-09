package com.hussein.mawaqit.data.infrastructure.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hussein.mawaqit.R
import com.hussein.mawaqit.data.infrastructure.services.AzanPlayerService
import com.hussein.mawaqit.data.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_TIME = "com.hussein.islamic.PRAYER_TIME"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"

        const val PRAYER_NOTIFICATION_CHANNEL_ID = "prayer_notifications"
        const val PRAYERS_NOTIFICATION_CHANNEL_NAME = "Prayer Times"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PRAYER_TIME) return


        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
        val isFajr = prayerName.equals("Fajr", ignoreCase = true)

        val soundPreference = runBlocking {
            SettingsRepository(context).settingsFlow.first().notificationSound
        }
        when {

            soundPreference == NotificationSound.ADHAN && !context.isScreenOn() -> {
                val serviceIntent = AzanPlayerService.buildIntent(context, prayerName, isFajr)
                ContextCompat.startForegroundService(context, serviceIntent)
            }

            else -> {
                showNotification(context, prayerName)
            }
        }
    }

    private fun showNotification(context: Context, prayerName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notification = NotificationCompat.Builder(context, PRAYER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_placeholder)
            .setContentTitle("🕌 $prayerName")
            .setContentText("It's time for $prayerName prayer")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        // Use a stable ID per prayer so simultaneous prayers don't overwrite each other
        val notificationId = prayerName.hashCode()
        notificationManager.notify(notificationId, notification)
    }


    private fun Context.isScreenOn(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive
}
