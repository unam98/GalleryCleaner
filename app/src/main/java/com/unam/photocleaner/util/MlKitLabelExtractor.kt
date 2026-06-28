package com.unam.photocleaner.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class MlKitLabelExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val labeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(0.65f).build()
    )

    suspend fun getLabels(uri: Uri): List<String> = suspendCancellableCoroutine { cont ->
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
