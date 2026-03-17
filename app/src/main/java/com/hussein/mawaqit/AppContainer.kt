package com.hussein.mawaqit

import CurrentLocationFetcher
import android.content.Context
import com.hussein.core.LocationRepository
import com.hussein.mawaqit.data.infrastructure.alarm_manager.PrayerAlarmManager
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.quran.QuranRepository

class AppContainer(private val context: Context) {

    val locationFetcher by lazy { CurrentLocationFetcher(context) }
    val locationRepository by lazy { LocationRepository(context) }
    val settingsRepository by lazy { SettingsRepository(context) }
    val alarmManager by lazy { PrayerAlarmManager(context) }
    val quranRepository by lazy { QuranRepository(context) }
}