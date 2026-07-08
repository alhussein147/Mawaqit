package com.hussein.mawaqit.infrastructure.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.hussein.mawaqit.infrastructure.notification.NotificationUtils
import com.hussein.mawaqit.infrastructure.settings.NotificationSound
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking


class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_TIME = "com.hussein.maqaqit.PRAYER_TIME"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
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
                NotificationUtils.showFullScreenPrayerNotification(context, prayerName, isFajr)
            }

            else -> {
                NotificationUtils.showPrayerNotification(context, prayerName)
            }
        }
    }


    private fun Context.isScreenOn(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive
}
