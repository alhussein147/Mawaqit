package com.hussein.mawaqit.infrastructure.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.hussein.mawaqit.MainActivity
import com.hussein.mawaqit.R
import com.hussein.mawaqit.presentation.azan.AzanActivity

object NotificationUtils {

    const val PRAYER_NOTIFICATION_CHANNEL_ID = "prayer_notifications"
    const val PRAYERS_NOTIFICATION_CHANNEL_NAME = "Prayer Times"

    const val AZAN_CHANNEL_ID = "azan_player"
    const val AZAN_CHANNEL_NAME = "Azan Player"

    const val SYNC_CHANNEL_ID = "data_sync"
    const val SYNC_CHANNEL_NAME = "Data Synchronization"

    const val OTHERS_CHANNEL_ID = "others"
    const val OTHERS_CHANNEL_NAME = "Others"

    private val Context.notificationManager: NotificationManager
        get() = getSystemService(NotificationManager::class.java)

    fun showPrayerNotification(context: Context, prayerName: String) {
        ensureChannelsCreated(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            mainIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, PRAYER_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle(context.getString(R.string.notification_prayer_title, prayerName))
            .setContentIntent(contentPendingIntent)
            .setContentText(context.getString(R.string.notification_prayer_content, prayerName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        context.notificationManager.notify(prayerName.hashCode(), notification)
    }

    fun areChannelsEnabled(context: Context): Boolean {
        val manager = context.notificationManager

        if (!manager.areNotificationsEnabled()) return false

        fun isEnabled(channelId: String): Boolean {
            val channel = manager.getNotificationChannel(channelId) ?: return true
            return channel.importance != NotificationManager.IMPORTANCE_NONE
        }

        return isEnabled(AZAN_CHANNEL_ID) || isEnabled(PRAYER_NOTIFICATION_CHANNEL_ID)
    }

    fun ensureChannelsCreated(context: Context) {
        val manager = context.notificationManager

        // Prayer Channel: Silent notifications with high priority (heads-up)
        val prayerChannel = NotificationChannel(
            PRAYER_NOTIFICATION_CHANNEL_ID,
            PRAYERS_NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_prayers_description)
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        // Azan Channel: High importance for full-screen alarms
        val azanChannel = NotificationChannel(
            AZAN_CHANNEL_ID,
            AZAN_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_azan_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        // Sync Channel: Low importance for background tasks
        val syncChannel = NotificationChannel(
            SYNC_CHANNEL_ID,
            SYNC_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Channel for background data synchronization tasks"
        }

        // Others Channel: Default importance for misc tasks
        val othersChannel = NotificationChannel(
            OTHERS_CHANNEL_ID,
            OTHERS_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Channel for various background tasks status"
        }

        manager.createNotificationChannels(listOf(prayerChannel, azanChannel, syncChannel, othersChannel))
    }

    fun showWorkerCompletionNotification(
        context: Context,
        title: String,
        message: String,
        isSuccess: Boolean
    ) {
        ensureChannelsCreated(context)

        val notification = NotificationCompat.Builder(context, OTHERS_CHANNEL_ID)
            .setSmallIcon(if (isSuccess) R.drawable.ic_check else R.drawable.ic_error) // Assuming ic_error exists or fallback
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        context.notificationManager.notify(title.hashCode(), notification)
    }

    fun showFullScreenPrayerNotification(
        context: Context,
        prayerName: String,
        isFajr: Boolean
    ) {
        ensureChannelsCreated(context)

        val fullScreenIntent = Intent(context, AzanActivity::class.java).apply {
            putExtra(AzanActivity.EXTRA_PRAYER_NAME, prayerName)
            putExtra(AzanActivity.EXTRA_IS_FAJR, isFajr)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, AZAN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_prayer_title, prayerName))
            .setContentText(context.getString(R.string.notification_prayer_content, prayerName))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        context.notificationManager.notify(AzanActivity.NOTIFICATION_ID, notification)
    }
}
