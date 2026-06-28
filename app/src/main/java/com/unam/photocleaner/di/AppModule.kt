package com.unam.photocleaner.di

import android.content.Context
import androidx.room.Room
import com.unam.photocleaner.data.local.db.AppDatabase
import com.unam.photocleaner.data.local.db.PhotoHashDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "photo_cleaner.db").build()

    @Provides
    @Singleton
    fun providePhotoHashDao(db: AppDatabase): PhotoHashDao = db.photoHashDao()
}
