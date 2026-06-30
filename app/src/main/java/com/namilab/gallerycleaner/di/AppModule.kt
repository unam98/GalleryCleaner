package com.namilab.gallerycleaner.di

import android.content.Context
import androidx.room.Room
import com.namilab.gallerycleaner.data.local.db.AppDatabase
import com.namilab.gallerycleaner.data.local.db.FavoritePhotoDao
import com.namilab.gallerycleaner.data.local.db.PhotoHashDao
import com.namilab.gallerycleaner.data.local.db.PhotoLabelDao
import com.namilab.gallerycleaner.ads.AdGateImpl
import com.namilab.gallerycleaner.domain.AdGate
import com.namilab.gallerycleaner.domain.GroupResultsCache
import com.namilab.gallerycleaner.domain.ScanNotifier
import com.namilab.gallerycleaner.notification.NotificationHelper
import com.namilab.gallerycleaner.presentation.ScanResultsCache
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

    @Binds
    @Singleton
    abstract fun bindGroupResultsCache(impl: ScanResultsCache): GroupResultsCache

    @Binds
    @Singleton
    abstract fun bindAdGate(impl: AdGateImpl): AdGate
}
