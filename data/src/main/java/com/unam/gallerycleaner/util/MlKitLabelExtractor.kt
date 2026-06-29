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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
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

    suspend fun getLabelsForPhotos(
        photos: List<Photo>,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
    ): Map<Long, List<String>> = coroutineScope {
        val cached = photoLabelDao.getByPhotoIds(photos.map { it.id })
            .associate { it.photoId to it.labels.split(",").filter { l -> l.isNotEmpty() } }

        val uncached = photos.filter { it.id !in cached }
        val total = photos.size
        val done = AtomicInteger(total - uncached.size)
        onProgress(done.get(), total)

        val semaphore = Semaphore(LABEL_CONCURRENCY)
        val fresh = ConcurrentHashMap<Long, List<String>>()

        uncached.map { photo ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val labels = runMlKit(photo.uri)
                    fresh[photo.id] = labels
                    onProgress(done.incrementAndGet(), total)
                }
            }
        }.awaitAll()

        if (fresh.isNotEmpty()) {
            photoLabelDao.upsertAll(fresh.map { (id, labels) ->
                PhotoLabelEntity(photoId = id, labels = labels.joinToString(","))
            })
        }

        cached + fresh
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

    companion object {
        private const val LABEL_CONCURRENCY = 6
    }
}
