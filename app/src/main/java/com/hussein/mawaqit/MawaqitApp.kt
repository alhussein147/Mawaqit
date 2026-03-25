package com.hussein.mawaqit

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.hussein.coreModule
import com.hussein.mawaqit.data.infrastructure.receivers.PrayerAlarmReceiver.Companion.PRAYERS_NOTIFICATION_CHANNEL_NAME
import com.hussein.mawaqit.data.infrastructure.receivers.PrayerAlarmReceiver.Companion.PRAYER_NOTIFICATION_CHANNEL_ID
import com.hussein.mawaqit.data.infrastructure.services.AzanPlayerService.Companion.AZAN_CHANNEL_ID
import com.hussein.mawaqit.data.infrastructure.services.AzanPlayerService.Companion.AZAN_CHANNEL_NAME
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

class MawaqitApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MawaqitApp)
            workManagerFactory()
            modules(
                settingsModule, homeModule, azkarModule, quranModule, recitationModule,
                coreModule, onboardingModule, workersModule
            )
        }
        createPrayerNotificationChannel()
        createAzanNotificationChannel()
    }

    private fun createPrayerNotificationChannel() {
        val name = PRAYERS_NOTIFICATION_CHANNEL_NAME
        val descriptionText = "Notifications for each of the 5 daily prayer times"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(PRAYER_NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
            enableVibration(true)
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createAzanNotificationChannel() {
        val channel = NotificationChannel(
            AZAN_CHANNEL_ID,
            AZAN_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Plays azan audio at prayer times"
            setSound(null, null) // silence the channel's own sound — we play via MediaPlayer
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)

    }
}
