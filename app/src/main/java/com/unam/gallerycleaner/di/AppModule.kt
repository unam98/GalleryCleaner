package com.unam.gallerycleaner.di

import android.content.Context
import androidx.room.Room
import com.unam.gallerycleaner.data.local.db.AppDatabase
import com.unam.gallerycleaner.data.local.db.FavoritePhotoDao
import com.unam.gallerycleaner.data.local.db.PhotoHashDao
import com.unam.gallerycleaner.data.local.db.PhotoLabelDao
import com.unam.gallerycleaner.domain.ScanNotifier
import com.unam.gallerycleaner.notification.NotificationHelper
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "gallery_cleaner.db")
            .addMigrations(AppDatabase.MIGRATION_3_4)
            .build()

    @Provides
    @Singleton
    fun providePhotoHashDao(db: AppDatabase): PhotoHashDao = db.photoHashDao()

    @Provides
    @Singleton
    fun provideFavoritePhotoDao(db: AppDatabase): FavoritePhotoDao = db.favoritePhotoDao()

    @Provides
    @Singleton
    fun providePhotoLabelDao(db: AppDatabase): PhotoLabelDao = db.photoLabelDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindScanNotifier(impl: NotificationHelper): ScanNotifier
}
