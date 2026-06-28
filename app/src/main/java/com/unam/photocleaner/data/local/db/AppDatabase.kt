package com.unam.photocleaner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PhotoHashEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoHashDao(): PhotoHashDao
}
