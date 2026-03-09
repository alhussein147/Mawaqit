package com.hussein.mawaqit.data.infrastructure.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.hussein.mawaqit.data.infrastructure.receivers.PrayerAlarmReceiver
import com.hussein.mawaqit.data.infrastructure.services.AzanPlayerService

object NotificationUtils {


    private fun ensureChannelExists(
        manager: NotificationManager,
        channelId: String,
        channelName: String,
        channelDescription: String
    ) {
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = channelDescription
            setSound(null, null) // sound is handled by AzanPlayerService, not the channel
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }


    /**
     * Returns false if the user has blocked the relevant notification channel
     * in system settings, so we don't waste an AlarmManager slot on it.
     *
     * - Fajr → checked against [AzanPlayerService.CHANNEL_ID] (azan_player)
     * - Others → checked against [PrayerAlarmReceiver.CHANNEL_ID] (prayer_notifications)
     *   but if the screen was off at alarm time, AzanPlayerService.CHANNEL_ID is used instead.
     *   We check both — if either is enabled, scheduling is worthwhile.
     */
    fun areChannelEnabled(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java)

        // If global notifications are disabled, nothing will show
        if (!manager.areNotificationsEnabled()) return false

        fun channelEnabled(channelId: String): Boolean {
            val channel = manager.getNotificationChannel(channelId) ?: return true
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }

        // At alarm time the receiver picks either AzanPlayerService (screen off)
        // or the silent prayer_notifications channel (screen on).
        // Schedule the alarm if at least one of those paths is open.
        val azanChannelOpen = channelEnabled(AzanPlayerService.AZAN_CHANNEL_ID)
        val silentChannelOpen = channelEnabled(PrayerAlarmReceiver.PRAYER_NOTIFICATION_CHANNEL_ID)

        return azanChannelOpen || silentChannelOpen
    }
}