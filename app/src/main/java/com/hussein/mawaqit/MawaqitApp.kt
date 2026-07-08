package com.hussein.mawaqit

import android.app.Application
import com.hussein.coreModule
import com.hussein.mawaqit.di.appModule
import com.hussein.mawaqit.infrastructure.notification.NotificationUtils
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
                appModule, coreModule
            )
        }
        NotificationUtils.ensureChannelsCreated(this)
    }


}
