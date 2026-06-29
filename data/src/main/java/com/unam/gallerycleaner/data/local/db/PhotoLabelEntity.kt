package com.unam.gallerycleaner.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_labels")
data class PhotoLabelEntity(
    @PrimaryKey val photoId: Long,
    val labels: String, // comma-joined ML Kit label strings
)
