package com.hussein.mawaqit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hussein.mawaqit.data.db.dao.AudioSourceDao
import com.hussein.mawaqit.data.db.dao.AyahDao
import com.hussein.mawaqit.data.db.dao.BookmarkDao
import com.hussein.mawaqit.data.db.dao.InvocationDao
import com.hussein.mawaqit.data.db.dao.SurahDao
import com.hussein.mawaqit.data.db.dao.TafsirDao
import com.hussein.mawaqit.data.db.entities.AudioSourceEntity
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.BookmarkEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity
import com.hussein.mawaqit.data.db.entities.invocation.InvocationCategoryEntity
import com.hussein.mawaqit.data.db.entities.invocation.InvocationEntity


@Database(
    entities = [
        SurahEntity::class,
        AyahEntity::class,
        BookmarkEntity::class,
        TafsirEntity::class,
        TafsirSourceEntity::class,
        AudioSourceEntity::class,
        InvocationEntity::class,
        InvocationCategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tafsirDao(): TafsirDao
    abstract fun audioSourceDao(): AudioSourceDao
    abstract fun invocationDao(): InvocationDao

    companion object {
        const val DB_NAME = "mawaqit.db"

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
