package com.hussein.mawaqit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.hussein.mawaqit.data.db.dao.AyahDao
import com.hussein.mawaqit.data.db.dao.BookmarkDao
import com.hussein.mawaqit.data.db.dao.SurahDao
import com.hussein.mawaqit.data.db.dao.TafsirDao
import com.hussein.mawaqit.data.db.entities.AyahEntity
import com.hussein.mawaqit.data.db.entities.BookmarkEntity
import com.hussein.mawaqit.data.db.entities.SurahEntity
import com.hussein.mawaqit.data.db.entities.TafsirEntity
import com.hussein.mawaqit.data.db.entities.TafsirSourceEntity


@Database(
    entities = [SurahEntity::class, AyahEntity::class, BookmarkEntity::class, TafsirEntity::class, TafsirSourceEntity::class],
    version = 1,
    exportSchema = false
)
// TODO:  rename 
abstract class QuranDatabase : RoomDatabase() {
    
    abstract fun surahDao(): SurahDao
    abstract fun ayahDao(): AyahDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun tafsirDao(): TafsirDao

    companion object {
        const val DB_NAME = "mawaqit.db"
        fun create(context: Context): QuranDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                QuranDatabase::class.java,
                DB_NAME
            ).build()
    }
}
