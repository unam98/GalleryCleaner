package com.unam.gallerycleaner

import android.net.Uri
import com.unam.gallerycleaner.data.local.db.PhotoHashEntity
import com.unam.gallerycleaner.domain.model.GroupType
import com.unam.gallerycleaner.domain.model.Photo
import com.unam.gallerycleaner.domain.model.PhotoGroup
import io.mockk.every
import io.mockk.mockk

object PhotoFactory {

    fun photo(
        id: Long = 1L,
        displayName: String = "photo_$id.jpg",
        dateTaken: Long = System.currentTimeMillis(),
        size: Long = 1_000_000L,
        mimeType: String = "image/jpeg",
        duration: Long = 0L,
    ): Photo {
        val uri = mockk<Uri>(relaxed = true)
        every { uri.toString() } returns "content://media/external/images/media/$id"
        return Photo(
            id = id,
            uri = uri,
            displayName = displayName,
            dateTaken = dateTaken,
            size = size,
            width = 1920,
            height = 1080,
            mimeType = mimeType,
            duration = duration,
        )
    }

    fun group(
        id: String = "group_1",
        photos: List<Photo> = listOf(photo(1L), photo(2L)),
        bestPhotoId: Long = photos.first().id,
        type: GroupType = GroupType.BURST,
        savingBytes: Long = 500_000L,
    ): PhotoGroup = PhotoGroup(
        id = id,
        photos = photos,
        bestPhotoId = bestPhotoId,
        type = type,
        potentialSavingBytes = savingBytes,
    )

    fun hashEntity(
        photoId: Long = 1L,
        dHash: Long = 0L,
        sharpness: Double = 100.0,
    ): PhotoHashEntity = PhotoHashEntity(
        photoId = photoId,
        dHash = dHash,
        sharpness = sharpness,
    )
}
