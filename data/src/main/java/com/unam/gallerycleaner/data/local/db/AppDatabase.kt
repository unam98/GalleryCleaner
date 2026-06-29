package com.unam.gallerycleaner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PhotoHashEntity::class, FavoritePhotoEntity::class, PhotoLabelEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun photoHashDao(): PhotoHashDao
    abstract fun favoritePhotoDao(): FavoritePhotoDao
    abstract fun photoLabelDao(): PhotoLabelDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS photo_labels (photoId INTEGER NOT NULL PRIMARY KEY, labels TEXT NOT NULL)"
                )
            }
        }
    }
}
