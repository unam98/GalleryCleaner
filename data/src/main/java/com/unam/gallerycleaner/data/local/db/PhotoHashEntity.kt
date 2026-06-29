package com.unam.gallerycleaner.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photo_hashes")
data class PhotoHashEntity(
    @PrimaryKey val photoId: Long,
    val dHash: Long,
    val sharpness: Double,
    val embedding: ByteArray? = null,  // TFLite FloatArray, serialized as little-endian bytes
) {
    override fun equals(other: Any?) = other is PhotoHashEntity && photoId == other.photoId
    override fun hashCode() = photoId.hashCode()
}
