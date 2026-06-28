package com.unam.photocleaner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PhotoHashEntity::class, FavoritePhotoEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoHashDao(): PhotoHashDao
    abstract fun favoritePhotoDao(): FavoritePhotoDao
}
