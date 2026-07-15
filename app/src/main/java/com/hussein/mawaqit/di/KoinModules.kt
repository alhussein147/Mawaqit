package com.hussein.mawaqit.di

import androidx.work.WorkManager
import com.hussein.core.LocationRepository
import com.hussein.mawaqit.data.RecitationRepository
import com.hussein.mawaqit.data.db.AppDatabase
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.data.db.repo.AzkarRepository
import com.hussein.mawaqit.data.db.repo.QuranDatabaseRepository
import com.hussein.mawaqit.data.db.repo.TafsirRepository
import com.hussein.mawaqit.domain.location.RefreshLocationUseCase
import com.hussein.mawaqit.infrastructure.alarm_manager.PrayerAlarmManager
import com.hussein.mawaqit.infrastructure.connectivity.NetworkObserver
import com.hussein.mawaqit.infrastructure.fonts.DynamicFontManager
import com.hussein.mawaqit.infrastructure.location.CurrentLocationFetcher
import com.hussein.mawaqit.infrastructure.media.AyahPlayer
import com.hussein.mawaqit.infrastructure.settings.QuranReaderPreferences
import com.hussein.mawaqit.infrastructure.settings.SettingsRepository
import com.hussein.mawaqit.infrastructure.workers.DatabasePopulationWorker
import com.hussein.mawaqit.infrastructure.workers.FontDownloadWorker
import com.hussein.mawaqit.infrastructure.workers.SurahDownloadWorker
import com.hussein.mawaqit.infrastructure.workers.population_workers.DataPopulationStrategy
import com.hussein.mawaqit.infrastructure.workers.population_workers.GenericPopulationWorker
import com.hussein.mawaqit.infrastructure.workers.population_workers.strategies.AudioSourcePopulationStrategy
import com.hussein.mawaqit.infrastructure.workers.population_workers.strategies.AzkarPopulationStrategy
import com.hussein.mawaqit.infrastructure.workers.population_workers.strategies.QuranPopulationStrategy
import com.hussein.mawaqit.infrastructure.workers.population_workers.strategies.TafsirPopulationStrategy
import com.hussein.mawaqit.infrastructure.workers.prayer.DailyPrayerWorker
import com.hussein.mawaqit.infrastructure.workers.prayer.PrayerSchedulerManager
import com.hussein.mawaqit.presentation.azkar.AzkarViewModel
import com.hussein.mawaqit.presentation.home.HomeViewModel
import com.hussein.mawaqit.presentation.onboarding.OnboardingViewModel
import com.hussein.mawaqit.presentation.qiblah.QiblahViewModel
import com.hussein.mawaqit.presentation.quran.bookmarks.BookmarksViewModel
import com.hussein.mawaqit.presentation.quran.reading.QuranReadingViewModel
import com.hussein.mawaqit.presentation.quran.reading.reading_settings.QuranSettingsViewModel
import com.hussein.mawaqit.presentation.quran.search.QuranSearchViewModel
import com.hussein.mawaqit.presentation.quran.surah_list.SurahListViewModel
import com.hussein.mawaqit.presentation.quran.tafsir.QuranReadingWithTafsirViewModel
import com.hussein.mawaqit.presentation.radio.RadioViewModel
import com.hussein.mawaqit.presentation.settings.SettingsViewModel
import com.hussein.mawaqit.presentation.util.GlobalPlayerViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.dsl.worker
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

@OptIn(KoinExperimentalAPI::class)
val appModule = module {
    // Infrastructure & Settings
    single { SettingsRepository(androidContext()) }
    single { DynamicFontManager(androidContext(), get()) }
    single { CurrentLocationFetcher(androidContext()) }
    single { RefreshLocationUseCase(get(), get(), get()) }
    single { WorkManager.getInstance(androidContext()) }
    factory { PrayerSchedulerManager(get()) }
    factory { LocationRepository(context = androidContext()) }
    factory { NetworkObserver(androidContext()) }
    factory { PrayerAlarmManager(androidContext()) }

    // Repositories & Data
    factory { AzkarRepository(androidContext(), get()) }
    single { RecitationRepository(androidContext(), get()) }
    factory { TafsirRepository(get()) }
    factory { QuranReaderPreferences(androidContext()) }
    factory { AyahPlayer(androidContext()) }

    // Quran Database
    single { AppDatabase.create(androidContext()) }
    single { get<AppDatabase>().surahDao() }
    single { get<AppDatabase>().ayahDao() }
    single { get<AppDatabase>().bookmarkDao() }
    single { get<AppDatabase>().tafsirDao() }
    single { get<AppDatabase>().azkarDao() }
    single { get<AppDatabase>().audioSourceDao() }
    factory { QuranDatabaseRepository(get(), get(), get()) }

    // Population Strategies
    factory { (source: TafsirSourceEntity) -> TafsirPopulationStrategy(get(), source) }
    factory<DataPopulationStrategy>(named("quran")) { QuranPopulationStrategy(get()) }
    factory<DataPopulationStrategy>(named("tafsir")) { params ->
        get<TafsirPopulationStrategy> { parametersOf(params.getOrNull<TafsirSourceEntity>() ?: TafsirSourceEntity.MUKHTASAR) }
    }
    factory<DataPopulationStrategy>(named("azkar")) { AzkarPopulationStrategy(get()) }
    factory<DataPopulationStrategy>(named("audio_source")) { AudioSourcePopulationStrategy(get()) }
    single { getAll<DataPopulationStrategy>() }

    // ViewModels
    viewModel {
        SettingsViewModel(
            settingsRepository = get<SettingsRepository>(),
            locationRepository = get(),
            currentLocationFetcher = get(),
            prayerSchedulerManager = get(),
            prayerAlarmManager = get(),
        )
    }
    viewModel {
        HomeViewModel(
            locationRepo = get(),
            settingsRepository = get(),
            quranDatabaseRepository = get(),
            refreshLocationUseCase = get()
        )
    }
    viewModel {
        AzkarViewModel(
            azkarRepository = get(),
            workerManager = get(),
            networkObserver = get()
        )
    }
    viewModel {
        QuranReadingViewModel(
            recitationRepository = get(),
            tafsirRepository = get(),
            quranReaderPreferences = get(),
            networkObserver = get(),
            ayahPlayer = get(),
            quranDatabaseRepository = get()
        )
    }
    viewModel {
        QuranSettingsViewModel(
            quranReaderPreferences = get(),
            tafsirRepository = get(),
            workManager = get()
        )
    }
    viewModel {
        SurahListViewModel(
            quranDatabaseRepository = get(),
            quranReaderPreferences = get(),
            workerManager = get(),
            tafsirRepository = get()
        )
    }
    viewModel {
        OnboardingViewModel(
            locationRepo = get(),
            settingsRepository = get(),
            locationFetcher = get(),
            prayerSchedulerManager = get(), networkObserver = get(), get()
        )
    }
    viewModel {
        QuranSearchViewModel(repo = get())
    }
    viewModel {
        QuranReadingWithTafsirViewModel(
            tafsirRepository = get(),
            workManager = get()
        )
    }
    viewModel {
        BookmarksViewModel(quranDatabaseRepository = get())
    }

    viewModel {
        RadioViewModel(
            recitationRepository = get(),
            workManager = get()
        )
    }

    viewModel {
        QiblahViewModel(
            locationRepository = get(),
            context = androidContext()
        )
    }

    single {
        GlobalPlayerViewModel(
            application = androidApplication(),
            recitationRepository = get(),
            workManager = get()
        )
    }

    // Workers
    worker { SurahDownloadWorker(context = androidContext(), params = get()) }
    worker { DatabasePopulationWorker(context = androidContext(), params = get()) }
    worker { DailyPrayerWorker(appContext = androidContext(), params = get()) }
    worker { GenericPopulationWorker(context = androidContext(), params = get()) }
    worker { FontDownloadWorker(context = androidContext(), params = get()) }
}
