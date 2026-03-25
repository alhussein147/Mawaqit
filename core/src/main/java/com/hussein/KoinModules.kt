package com.hussein

import com.hussein.core.LocationRepository
import com.hussein.core.PrayerTimeCalculator
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val coreModule = module {
    single { LocationRepository(androidContext()) }
    single { PrayerTimeCalculator }
}