package com.hussein.mawaqit.di

import androidx.work.WorkManager
import com.hussein.core.LocationRepository
import com.hussein.mawaqit.data.azkar.AzkarRepository
import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.data.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.data.quran.recitation.SurahDownloadRepository
import com.hussein.mawaqit.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.infrastructure.connectivity.NetworkObserver
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.infrastructure.workers.DailyPrayerWorker
import com.hussein.mawaqit.infrastructure.workers.DatabasePopulationWorker
import com.hussein.mawaqit.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.infrastructure.workers.local_population_workers.QuranPopulationWorker
import com.hussein.mawaqit.infrastructure.workers.local_population_workers.TafsirPopulationWorker
import com.hussein.mawaqit.presentation.azkar.AzkarViewModel
import com.hussein.mawaqit.presentation.home.HomeViewModel
import com.hussein.mawaqit.presentation.onboarding.OnboardingViewModel
import com.hussein.mawaqit.presentation.quran.bookmarks.BookmarksViewModel
import com.hussein.mawaqit.presentation.quran.list_screen.SurahListViewModel
import com.hussein.mawaqit.presentation.quran.reader.QuranSettingsViewModel
import com.hussein.mawaqit.presentation.quran.reader.QuranViewModel
import com.hussein.mawaqit.presentation.quran.search.QuranSearchViewModel
import com.hussein.mawaqit.presentation.quran.tafsir.QuranReaderWithTafsirViewModel
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
    single { SurahDownloadRepository(androidContext()) }
    factory { TafsirRepository(get()) }
    factory { QuranReaderPreferences(androidContext()) }
    factory { AyahPlayer(androidContext()) }

    // Quran Database
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().surahDao() }
    single { get<AppDatabase>().ayahDao() }
    single { get<AppDatabase>().bookmarkDao() }
    factory { QuranDatabaseRepository(get(), get(), get()) }

    // ViewModels
    viewModel {
        SettingsViewModel(
            settingsRepository = get<SettingsRepository>(),
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
            surahDownloadRepository = get(),
            tafsirRepository = get(),
            quranReaderPreferences = get(),
            networkObserver = get(),
            ayahPlayer = get(),
            quranDatabaseRepository = get()
        )
    }
    viewModel {
        QuranSettingsViewModel(
            quranReaderPreferences = get()
        )
    }
    viewModel {
        SurahListViewModel(
            quranDatabaseRepository = get(),
            quranReaderPreferences = get()
        )
    }
    viewModel {
        OnboardingViewModel(
            locationRepo = get(),
            settingsRepository = get(),
            locationFetcher = get(),
            workerManager = get(),
            prayerSchedulerManager = get(), networkObserver = get()
        )
    }
    viewModel {
        QuranSearchViewModel(repo = get())
    }
    viewModel {
        QuranReaderWithTafsirViewModel(tafsirRepository = get())
    }
    viewModel {
        BookmarksViewModel(quranDatabaseRepository = get())
    }

    single {
        GlobalPlayerViewModel(
            application = androidApplication(),
            surahDownloadRepository = get(),
            workManager = get()
        )
    }

    // Workers
    worker { SurahDownloadWorker(context = androidContext(), params = get()) }
    worker { DatabasePopulationWorker(context = androidContext(), params = get()) }
    worker { DailyPrayerWorker(appContext = androidContext(), params = get()) }
    worker { TafsirPopulationWorker(appContext = androidContext(), params = get()) }
    worker { QuranPopulationWorker(context = androidContext(), params = get()) }
}
