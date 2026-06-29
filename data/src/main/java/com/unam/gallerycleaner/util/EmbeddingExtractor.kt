package com.unam.gallerycleaner.util

import android.content.Context
import android.graphics.Bitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class EmbeddingExtractor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var interpreter: Interpreter? = null
    private var outputDim: Int = 0

    @Synchronized
    fun initialize(): Boolean {
        if (interpreter != null) return true
        return try {
            val afd = context.assets.openFd(MODEL_FILE)
            val buffer = FileInputStream(afd.fileDescriptor).channel.map(
                java.nio.channels.FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength,
            )
            val interp = Interpreter(buffer, Interpreter.Options().apply { numThreads = 2 })
            outputDim = interp.getOutputTensor(0).shape()[1]
            interpreter = interp
            true
        } catch (e: Exception) {
            false
        }
    }

    fun extract(bitmap: Bitmap): FloatArray? {
        val interp = interpreter ?: return null
        val resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        resized.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val inputBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4).apply {
            order(ByteOrder.nativeOrder())
        }
        // getPixels() is a single JNI call vs 50K individual getPixel() calls
        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 127.5f - 1f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 127.5f - 1f)
            inputBuffer.putFloat((pixel and 0xFF) / 127.5f - 1f)
        }
        resized.recycle()

        val output = Array(1) { FloatArray(outputDim) }
        interp.run(inputBuffer, output)
        return output[0]
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f; var normA = 0f; var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]; normA += a[i] * a[i]; normB += b[i] * b[i]
        }
        val denom = sqrt(normA) * sqrt(normB)
        return if (denom == 0f) 0f else dot / denom
    }

    fun isReady() = interpreter != null

    companion object {
        private const val MODEL_FILE = "mobilenet_v3_small.tflite"
        private const val INPUT_SIZE = 224
    }
}
