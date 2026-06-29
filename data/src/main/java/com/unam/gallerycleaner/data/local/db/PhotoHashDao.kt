package com.unam.gallerycleaner.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoHashDao {

    @Query("SELECT * FROM photo_hashes WHERE photoId = :photoId")
    suspend fun get(photoId: Long): PhotoHashEntity?

    @Query("SELECT * FROM photo_hashes")
    suspend fun getAll(): List<PhotoHashEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoHashEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PhotoHashEntity>)

    @Query("DELETE FROM photo_hashes WHERE photoId NOT IN (:validIds)")
    suspend fun deleteStale(validIds: List<Long>)
}
