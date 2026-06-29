package com.unam.gallerycleaner.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface PhotoLabelDao {

    @Query("SELECT * FROM photo_labels WHERE photoId IN (:photoIds)")
    suspend fun getByPhotoIds(photoIds: List<Long>): List<PhotoLabelEntity>

    @Upsert
    suspend fun upsertAll(entities: List<PhotoLabelEntity>)
}
