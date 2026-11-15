package com.hussein.mawaqit

import android.app.Application
import androidx.work.Configuration
import com.hussein.coreModule
import com.hussein.mawaqit.di.appModule
import com.hussein.mawaqit.infrastructure.notification.NotificationUtils
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.factory.KoinWorkerFactory
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin

class MawaqitApp : Application(), Configuration.Provider, KoinComponent {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@MawaqitApp)
            workManagerFactory()
            modules(
                appModule, coreModule
            )
        }
        NotificationUtils.ensureChannelsCreated(this)
        try {
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(
                com.hussein.mawaqit.data.remote.RemoteService.sslContext.socketFactory
            )
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(get<KoinWorkerFactory>())
            .build()
}
