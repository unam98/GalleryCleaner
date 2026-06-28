package com.unam.photocleaner.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_hashes")
data class PhotoHashEntity(
    @PrimaryKey val photoId: Long,
    val dHash: Long,
    val sharpness: Double,
)
