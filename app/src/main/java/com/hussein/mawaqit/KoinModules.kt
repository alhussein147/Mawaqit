package com.hussein.mawaqit

import CurrentLocationFetcher
import androidx.work.WorkManager
import com.hussein.core.LocationRepository
import com.hussein.mawaqit.data.azkar.AzkarRepository
import com.hussein.mawaqit.data.db.QuranDatabase
import com.hussein.mawaqit.data.db.QuranDatabaseRepository
import com.hussein.mawaqit.data.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.data.infrastructure.network.NetworkObserver
import com.hussein.mawaqit.data.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.data.infrastructure.workers.DailyPrayerWorker
import com.hussein.mawaqit.data.infrastructure.workers.QuranPopulationWorker
import com.hussein.mawaqit.data.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.data.quran.QuranDisplayPreferences
import com.hussein.mawaqit.data.quran.recitation.RecitationRepository
import com.hussein.mawaqit.data.quran.tafsir.TafsirRepository
import com.hussein.mawaqit.presentation.azkar.AzkarViewModel
import com.hussein.mawaqit.presentation.home.HomeViewModel
import com.hussein.mawaqit.presentation.onboarding.OnboardingViewModel
import com.hussein.mawaqit.presentation.quran.list_screen.SurahListViewModel
import com.hussein.mawaqit.presentation.quran.reader.QuranViewModel
import com.hussein.mawaqit.presentation.quran.search.QuranSearchViewModel
import com.hussein.mawaqit.presentation.settings.SettingsViewModel
import com.hussein.mawaqit.presentation.util.GlobalPlayerViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@OptIn(KoinExperimentalAPI::class)
val appModule = module {
    // Infrastructure & Settings
    single { SettingsRepository(androidContext()) }
    single { CurrentLocationFetcher(androidContext()) }
    single { WorkManager.getInstance(androidContext()) }
    factory { PrayerSchedulerManager(get()) }
    factory { LocationRepository(context = androidContext()) }
    factory { NetworkObserver(androidContext()) }

    // Repositories & Data
    factory { AzkarRepository(androidContext()) }
    single { RecitationRepository(androidContext()) }
    factory { TafsirRepository() }
    factory { QuranDisplayPreferences(androidContext()) }
    factory { AyahPlayer(androidContext()) }

    // Quran Database
    single { QuranDatabase.create(androidContext()) }
    single { get<QuranDatabase>().surahDao() }
    single { get<QuranDatabase>().ayahDao() }
    single { get<QuranDatabase>().bookmarkDao() }
    factory { QuranDatabaseRepository(get(), get(), get()) }

    // ViewModels
    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            locationRepository = get(),
            currentLocationFetcher = get(),
            prayerSchedulerManager = get()
        )
    }
    viewModel {
        HomeViewModel(
            locationRepo = get(),
            settingsRepository = get(),
            quranDatabaseRepository = get(),
        )
    }
    viewModel { AzkarViewModel(get()) }
    viewModel {
        QuranViewModel(
            recitationRepository = get(),
            tafsirRepository = get(),
            quranDisplayPreferences = get(),
            networkObserver = get(),
            ayahPlayer = get(),
            quranDatabaseRepository = get()
        )
    }
    viewModel {
        SurahListViewModel(
            quranDatabaseRepository = get(),
        )
    }

    single {
        GlobalPlayerViewModel(
            application = androidApplication(),
            recitationRepository = get(),
            workManager = get()
        )
    }
    viewModel {
        OnboardingViewModel(
            locationRepo = get(),
            settingsRepository = get(),
            locationFetcher = get(),
            workerManager = get(),
            prayerSchedulerManager = get()
        )
    }
    viewModel {
        QuranSearchViewModel(repo = get())
    }

    // Workers
    worker { SurahDownloadWorker(context = androidContext(), params = get()) }
    worker { QuranPopulationWorker(context = androidContext(), params = get()) }
    worker { DailyPrayerWorker(appContext = androidContext(), params = get()) }
}
