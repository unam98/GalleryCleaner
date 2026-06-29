package com.unam.gallerycleaner.util

import android.graphics.Bitmap

object ImageQualityUtils {

    // 라플라시안 분산 — 값이 클수록 선명
    // grayscale Bitmap 복사 없이 inline luma로 직접 계산 (Bitmap 할당 제거)
    fun sharpness(bitmap: Bitmap): Double {
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)

        var sum = 0.0
        var sumSq = 0.0
        var count = 0

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = luma(pixels[y * w + x]) * 4
                val top = luma(pixels[(y - 1) * w + x])
                val bottom = luma(pixels[(y + 1) * w + x])
                val left = luma(pixels[y * w + (x - 1)])
                val right = luma(pixels[y * w + (x + 1)])
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

    // BT.601 정수 근사: (77R + 150G + 29B) >> 8 — 부동소수점 없이 luma 계산
    private fun luma(px: Int): Int =
        ((px shr 16 and 0xFF) * 77 + (px shr 8 and 0xFF) * 150 + (px and 0xFF) * 29) shr 8
}
