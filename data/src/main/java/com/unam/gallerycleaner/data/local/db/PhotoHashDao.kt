package com.unam.gallerycleaner.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoHashDao {

    @Query("SELECT * FROM photo_hashes WHERE photoId = :photoId")
    suspend fun get(photoId: Long): PhotoHashEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PhotoHashEntity)

    @Query("DELETE FROM photo_hashes WHERE photoId NOT IN (:validIds)")
    suspend fun deleteStale(validIds: List<Long>)
}
