# Implementation Plan: Azkar Persistence and Remote Sync

This plan outlines the steps to persist Azkar data in the local database, fetch it from a remote CDN, and integrate it with the existing `GenericPopulationWorker` for background synchronization.

## Proposed Changes

### Database Layer

#### [NEW] [AzkarCategoryEntity.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/data/db/entities/AzkarCategoryEntity.kt)
Define the entity for Azkar categories.
- `id`: Int (Primary Key)
- `title`: String
- `highlight`: Boolean

#### [NEW] [AzkarItemEntity.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/data/db/entities/AzkarItemEntity.kt)
Define the entity for individual Zikr items.
- `id`: Int (Primary Key, Auto-increment)
- `categoryId`: Int (Foreign Key to `AzkarCategoryEntity.id`)
- `zekr`: String
- `repeat`: Int
- `bless`: String

#### [NEW] [AzkarDao.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/data/db/dao/AzkarDao.kt)
Interface for database operations:
- `insertCategories(List<AzkarCategoryEntity>)`
- `insertItems(List<AzkarItemEntity>)`
- `getAllCategories()`
- `getItemsForCategory(categoryId: Int)`
- `countCategories()`

#### [MODIFY] [AppDatabase.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/data/db/AppDatabase.kt)
- Add `AzkarCategoryEntity` and `AzkarItemEntity` to the `@Database` annotation.
- Increment `version` to `2`.
- Add `azkarDao()` abstract method.
- Add `MIGRATION_1_2` to create the two new tables.

---

### Network and Data Layer

#### [NEW] [AzkarDto.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/data/remote/dto/AzkarDto.kt)
Define DTOs for remote CDN response:
- `AzkarCategoryDto`: Maps to the JSON object provided.
- `AzkarItemDto`: Maps to the items within the `content` list.

#### [MODIFY] [AzkarRepository.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/data/azkar/AzkarRepository.kt)
- Update to include methods for fetching from CDN.
- Methods to interact with the database via `AzkarDao`.

---

### Infrastructure Layer (Sync)

#### [NEW] [AzkarPopulationStrategy.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/infrastructure/workers/local_population_workers/strategies/AzkarPopulationStrategy.kt)
Implement `DataPopulationStrategy`:
- `name`: "Azkar"
- `shouldPopulate()`: Returns true if Azkar tables are empty.
- `execute()`: Fetches data from CDN and saves to the database.

#### [MODIFY] [KoinModules.kt](file:///F:/ProjectsFiles/AndroidStudio_Projects/Mawaqit/app/src/main/java/com/hussein/mawaqit/di/KoinModules.kt)
- Register `AzkarDao` in Koin.
- Register `AzkarPopulationStrategy` as a `DataPopulationStrategy`.

## Migration Plan

The database version will be incremented from `1` to `2`. A `Migration` object will be added to `AppDatabase` with the following SQL:

```sql
CREATE TABLE IF NOT EXISTS `azkar_categories` (
    `id` INTEGER NOT NULL,
    `title` TEXT NOT NULL,
    `highlight` INTEGER NOT NULL,
    PRIMARY KEY(`id`)
);

CREATE TABLE IF NOT EXISTS `azkar_items` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `categoryId` INTEGER NOT NULL,
    `zekr` TEXT NOT NULL,
    `repeat` INTEGER NOT NULL,
    `bless` TEXT NOT NULL,
    FOREIGN KEY(`categoryId`) REFERENCES `azkar_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
```

## Verification Plan

### Automated Tests
- Unit tests for `AzkarDao` to verify insertion and retrieval.
- Mock server tests for `AzkarPopulationStrategy` to ensure correct parsing and saving.

### Manual Verification
- Trigger the `GenericPopulationWorker` with `strategy_name="Azkar"` and verify that the database is populated correctly via App Inspection in Android Studio.
- Verify UI (if applicable) displays data from the database.
