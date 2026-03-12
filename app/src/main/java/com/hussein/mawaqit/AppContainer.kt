package com.hussein.mawaqit

import android.content.Context
import android.util.Log
import com.hussein.mawaqit.data.azkar.AzkarRepository
import com.hussein.mawaqit.data.infrastructure.alarm_manager.PrayerAlarmManager
import com.hussein.mawaqit.data.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.data.infrastructure.location.LocationRepository
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.quran.QuranRepository

class AppContainer(private val context: Context) {

    val TAG = "AppContainer"

    val locationRepository by lazy { LocationRepository(context) }
    val settingsRepository by lazy { SettingsRepository(context) }
    val alarmManager by lazy { PrayerAlarmManager(context) }
    val locationFetcher by lazy { CurrentLocationFetcher(context) }
    val quranRepository by lazy { QuranRepository(context) }


    // azakr repo should be viewmodel scoped
    var azkarRepository: AzkarRepository? = null

    fun createAzkarRepo(): AzkarRepository {
        azkarRepository = AzkarRepository(context)
        Log.d(TAG, "Created azakr repo")
        return azkarRepository!!
    }

    fun destroyAzkarRepo() {
        Log.d(TAG, "Destroyed azakr repo")
        azkarRepository = null

    }
}