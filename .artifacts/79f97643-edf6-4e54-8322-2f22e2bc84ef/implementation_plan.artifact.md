# Implement Empty State and Data Sync for SurahListScreen

This plan addresses the requirement to show an empty state in `SurahListScreen` when the Quran table is empty and provide a button to trigger a chained sync of Quran and Tafsir data using WorkManager.

## User Review Required

> [!IMPORTANT]
> The sync process will be chained: Quran data first, then Tafsir data. If the user starts the sync, they will see a progress indicator.

## Proposed Changes

### [Quran]

#### [MODIFY] [KoinModules.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/di/KoinModules.kt)
- Update `SurahListViewModel` injection to include `WorkManager`.

#### [MODIFY] [SurahListViewModel.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/presentation/quran/surah_list/SurahListViewModel.kt)
- Add `WorkManager` dependency.
- Add `syncStatus` to `SurahListUiState`.
- Implement `syncQuran()` method to chain Quran and Tafsir population workers.
- Add logic to observe WorkManager progress and update UI state.

#### [MODIFY] [SurahListScreen.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/presentation/quran/surah_list/SurahListScreen.kt)
- Add an empty state UI that appears when `surahs` is empty and `!isLoading`.
- The empty state will include a message and a "Download Quran Data" button.
- Show a progress indicator when syncing.

## Verification Plan

### Automated Tests
- N/A (UI and WorkManager interaction)

### Manual Verification
1.  Clear app data or ensure Quran table is empty.
2.  Open the Quran tab.
3.  Verify the empty state message and "Download Quran Data" button are visible.
4.  Click the button.
5.  Verify that the sync process starts (check notifications/logs).
6.  Verify that a progress indicator (or status message) is shown during sync.
7.  Verify that once sync completes, the Surah list is populated automatically.
