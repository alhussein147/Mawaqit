package com.hussein.mawaqit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hussein.Mawaqit.quran.db.AyahDao
import com.hussein.Mawaqit.quran.db.BookmarkDao
import com.hussein.Mawaqit.quran.db.SurahDao

@Database(
    entities = [SurahEntity::class, AyahEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        fun create(context: Context): QuranDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                "quran.db"
            ).build()
    }
}