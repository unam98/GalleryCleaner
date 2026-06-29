package com.unam.gallerycleaner.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_photos")
data class FavoritePhotoEntity(
    @PrimaryKey val photoId: Long,
)
