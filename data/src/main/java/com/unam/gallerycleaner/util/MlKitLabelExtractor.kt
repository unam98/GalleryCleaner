package com.unam.gallerycleaner.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.unam.gallerycleaner.data.local.db.PhotoLabelDao
import com.unam.gallerycleaner.data.local.db.PhotoLabelEntity
import com.unam.gallerycleaner.domain.model.Photo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitLabelExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoLabelDao: PhotoLabelDao,
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.65f).build()
    )

    // 배치 처리: 캐시 히트는 즉시 반환, 미스만 ML Kit 실행 후 저장
    suspend fun getLabelsForPhotos(photos: List<Photo>): Map<Long, List<String>> {
        val cached = photoLabelDao.getByPhotoIds(photos.map { it.id })
            .associate { it.photoId to it.labels.split(",").filter { l -> l.isNotEmpty() } }

        val uncached = photos.filter { it.id !in cached }
        val fresh = mutableMapOf<Long, List<String>>()
        uncached.forEach { photo ->
            val labels = runMlKit(photo.uri)
            fresh[photo.id] = labels
        }

        if (fresh.isNotEmpty()) {
            photoLabelDao.upsertAll(fresh.map { (id, labels) ->
                PhotoLabelEntity(photoId = id, labels = labels.joinToString(","))
            })
        }

        return cached + fresh
    }

    private suspend fun runMlKit(uri: Uri): List<String> = suspendCancellableCoroutine { cont ->
        try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
            if (bitmap == null) {
                cont.resume(emptyList())
                return@suspendCancellableCoroutine
            }
            val image = InputImage.fromBitmap(bitmap, 0)
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    if (cont.isActive) cont.resume(labels.map { it.text })
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(emptyList())
                }
        } catch (e: Exception) {
            if (cont.isActive) cont.resume(emptyList())
        }
    }
}
