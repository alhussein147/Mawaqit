package com.hussein.mawaqit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hussein.mawaqit.data.db.dao.AudioSourceDao
import com.hussein.mawaqit.data.db.dao.AyahDao
import com.hussein.mawaqit.data.db.dao.AzkarDao
import com.hussein.mawaqit.data.db.dao.BookmarkDao
import com.hussein.mawaqit.data.db.dao.SurahDao
import com.hussein.mawaqit.data.db.dao.TafsirDao
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.invocation.AzkarCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.AzkarItemEntity
import com.hussein.mawaqit.data.db.entities.BookmarkEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity


@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        BookmarkEntity::class,
        TafsirEntity::class,
        TafsirSourceEntity::class,
        AzkarCategoryEntity::class,
        AzkarItemEntity::class,
        AudioSourceEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tafsirDao(): TafsirDao
    abstract fun azkarDao(): AzkarDao
    abstract fun audioSourceDao(): AudioSourceDao

    companion object {
        const val DB_NAME = "mawaqit.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `azkar_categories` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `highlight` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `azkar_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `categoryId` INTEGER NOT NULL, `zekr` TEXT NOT NULL, `repeat` INTEGER, `bless` TEXT, FOREIGN KEY(`categoryId`) REFERENCES `azkar_categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
            }
        }
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .fallbackToDestructiveMigration(true)
                .setJournalMode(JournalMode.TRUNCATE)
                .build()

    }
}
