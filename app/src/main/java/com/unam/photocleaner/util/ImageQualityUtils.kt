package com.unam.photocleaner.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object ImageQualityUtils {

    // 라플라시안 분산 — 값이 클수록 선명
    fun sharpness(bitmap: Bitmap): Double {
        val gray = toGrayscale(bitmap)
        val result = laplacianVariance(gray)
        gray.recycle()
        return result
    }

    private fun toGrayscale(src: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().also { it.setSaturation(0f) })
        }
        canvas.drawBitmap(src, 0f, 0f, paint)
        return result
    }

    private fun laplacianVariance(bitmap: Bitmap): Double {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = (pixels[y * w + x] and 0xFF) * 4
                val top = pixels[(y - 1) * w + x] and 0xFF
                val bottom = pixels[(y + 1) * w + x] and 0xFF
                val left = pixels[y * w + (x - 1)] and 0xFF
                val right = pixels[y * w + (x + 1)] and 0xFF
                val lap = (center - top - bottom - left - right).toDouble()
                sum += lap
                sumSq += lap * lap
                count++
            }
        }
        if (count == 0) return 0.0
        val mean = sum / count
        return (sumSq / count) - mean * mean
    }
}
