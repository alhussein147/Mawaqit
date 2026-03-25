package com.hussein.mawaqit

import CurrentLocationFetcher
import com.hussein.mawaqit.data.azkar.AzkarRepository
import com.hussein.mawaqit.data.db.QuranDatabase
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.network.NetworkObserver
import com.hussein.mawaqit.data.quran.QuranDisplayPreferences
import com.hussein.mawaqit.data.recitation.RecitationRepository
import com.hussein.mawaqit.presentation.azkar.AzkarViewModel
import com.hussein.mawaqit.presentation.home.HomeViewModel
import com.hussein.mawaqit.presentation.onboarding.OnboardingViewModel
import com.hussein.mawaqit.presentation.quran.list_screen.SurahListViewModel
import com.hussein.mawaqit.presentation.quran.reader.AyahPlayer
import com.hussein.mawaqit.presentation.quran.reader.QuranViewModel
import com.hussein.mawaqit.presentation.quran.tafsir.TafsirRepository
import com.hussein.mawaqit.presentation.settings.SettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    single { SettingsRepository(androidContext()) }
    viewModel { SettingsViewModel(application = androidApplication()) }
}

@OptIn(KoinExperimentalAPI::class)
val homeModule = module {
    viewModel { HomeViewModel(get(), get()) }

}

val azkarModule = module {
    factory { AzkarRepository(androidContext()) }
    viewModel { AzkarViewModel(get()) }
}

val quranModule = module {
    single { QuranDatabase.create(androidContext()) }
    single { get<QuranDatabase>().surahDao() }
    single { get<QuranDatabase>().ayahDao() }
    single { get<QuranDatabase>().bookmarkDao() }
    single { QuranDatabaseRepository(androidContext(), get(), get(), get()) }

    factory { QuranDisplayPreferences(androidContext()) }
    factory { TafsirRepository() }
    factory { NetworkObserver(androidContext()) }
    factory { AyahPlayer(androidContext()) }
    viewModel { QuranViewModel(
        recitationRepository = get(),
        tafsirRepository = get(),
        quranDisplayPreferences = get(),
        networkObserver = get(),
        ayahPlayer = get(),
        quranDatabaseRepository = get()
    ) }
    single { SurahListViewModel(androidApplication()) }
}

val recitationModule = module {
    single { RecitationRepository(androidContext()) }
}
val onboardingModule = module {
    single { CurrentLocationFetcher(androidContext()) }
    viewModel {
        OnboardingViewModel(
            locationRepo = get(),
            settingsRepository = get(),
            locationFetcher = get(),
            application = get()
        )
    }
}
