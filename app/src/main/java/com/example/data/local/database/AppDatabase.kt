package com.example.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.DocumentDao
import com.example.data.local.dao.PageDao
import com.example.data.local.dao.ScanSessionDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.PageEntity
import com.example.data.local.entity.ScanSessionEntity
import com.example.data.local.entity.ScanSessionPageEntity

@Database(
    entities = [
        DocumentEntity::class,
        PageEntity::class,
        BookmarkEntity::class,
        ScanSessionEntity::class,
        ScanSessionPageEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao
    abstract fun pageDao(): PageDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun scanSessionDao(): ScanSessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "arabic_library_scanner.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
