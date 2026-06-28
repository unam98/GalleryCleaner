package com.unam.photocleaner.data.local

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
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
    suspend fun getAllPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
        )
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"
        val photos = mutableListOf<Photo>()

        context.contentResolver.query(collection, projection, null, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                photos.add(
                    Photo(
                        id = id,
                        uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                        displayName = cursor.getString(nameCol) ?: "",
                        dateTaken = cursor.getLong(dateCol),
                        size = cursor.getLong(sizeCol),
                        width = cursor.getInt(widthCol),
                        height = cursor.getInt(heightCol),
                        mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                    )
                )
            }
        }
        photos
    }

    // lastScanned 이후 새로 추가된 사진만 반환 (백그라운드 스캔용)
    suspend fun getPhotosSince(timestampMs: Long): List<Photo> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
        )
        val selection = "${MediaStore.Images.Media.DATE_TAKEN} >= ?"
        val selectionArgs = arrayOf(timestampMs.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} ASC"
        val photos = mutableListOf<Photo>()

        context.contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    photos.add(
                        Photo(
                            id = id,
                            uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                            ),
                            displayName = cursor.getString(nameCol) ?: "",
                            dateTaken = cursor.getLong(dateCol),
                            size = cursor.getLong(sizeCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                        )
                    )
                }
            }
        photos
    }

    // Android 10+: 시스템 삭제 다이얼로그용 IntentSender 반환
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun createDeleteRequest(ids: List<Long>): IntentSender = withContext(Dispatchers.IO) {
        val uris = ids.map { ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it) }
        MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    // Android 8-9: 직접 삭제 (WRITE_EXTERNAL_STORAGE 권한 필요)
    suspend fun deletePhotos(ids: List<Long>): Int = withContext(Dispatchers.IO) {
        ids.count { id ->
            val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
            context.contentResolver.delete(uri, null, null) > 0
        }
    }
}
