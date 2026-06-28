package com.unam.photocleaner.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritePhotoDao {

    @Query("SELECT photoId FROM favorite_photos")
    fun observeAll(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FavoritePhotoEntity)

    @Query("DELETE FROM favorite_photos WHERE photoId = :photoId")
    suspend fun delete(photoId: Long)
}
