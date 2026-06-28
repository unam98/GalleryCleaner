package com.unam.photocleaner.data.local

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.unam.photocleaner.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getAllPhotos(
        sinceMs: Long? = null,
        minSizeBytes: Long = 0L,
        maxCount: Int? = null,
    ): List<Photo> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
        )
        val (selection, args) = buildFilter(
            dateCol = MediaStore.Images.Media.DATE_TAKEN,
            sizeCol = MediaStore.Images.Media.SIZE,
            sinceMs = sinceMs,
            minSizeBytes = minSizeBytes,
        )
        val photos = mutableListOf<Photo>()
        context.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Images.Media.DATE_TAKEN} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                photos.add(Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    displayName = cursor.getString(nameCol) ?: "",
                    dateTaken = cursor.getLong(dateCol),
                    size = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                ))
            }
        }
        if (maxCount != null) photos.takeLast(maxCount) else photos
    }

    suspend fun getAllVideos(
        sinceMs: Long? = null,
        minSizeBytes: Long = 0L,
        maxCount: Int? = null,
    ): List<Photo> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DURATION,
        )
        val (selection, args) = buildFilter(
            dateCol = MediaStore.Video.Media.DATE_TAKEN,
            sizeCol = MediaStore.Video.Media.SIZE,
            sinceMs = sinceMs,
            minSizeBytes = minSizeBytes,
        )
        val videos = mutableListOf<Photo>()
        context.contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Video.Media.DATE_TAKEN} ASC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                videos.add(Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                    displayName = cursor.getString(nameCol) ?: "",
                    dateTaken = cursor.getLong(dateCol),
                    size = cursor.getLong(sizeCol),
                    width = cursor.getInt(widthCol),
                    height = cursor.getInt(heightCol),
                    mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                    duration = cursor.getLong(durCol),
                ))
            }
        }
        if (maxCount != null) videos.takeLast(maxCount) else videos
    }

    suspend fun getPhotosSince(timestampMs: Long): List<Photo> = getAllPhotos(sinceMs = timestampMs)

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun createDeleteRequest(uris: List<Uri>): IntentSender = withContext(Dispatchers.IO) {
        MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    suspend fun deleteMedia(uris: List<Uri>): Int = withContext(Dispatchers.IO) {
        uris.count { uri -> context.contentResolver.delete(uri, null, null) > 0 }
    }

    private fun buildFilter(
        dateCol: String,
        sizeCol: String,
        sinceMs: Long?,
        minSizeBytes: Long,
    ): Pair<String?, Array<String>?> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()
        sinceMs?.let { conditions += "$dateCol >= ?"; args += it.toString() }
        if (minSizeBytes > 0) { conditions += "$sizeCol >= ?"; args += minSizeBytes.toString() }
        return if (conditions.isEmpty()) null to null
        else conditions.joinToString(" AND ") to args.toTypedArray()
    }
}
