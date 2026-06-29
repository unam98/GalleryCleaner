package com.unam.gallerycleaner.util

import android.graphics.Bitmap

object DHashUtils {

    private const val HASH_SIZE = 8

    fun compute(bitmap: Bitmap): Long {
        val resized = Bitmap.createScaledBitmap(bitmap, HASH_SIZE + 1, HASH_SIZE, false)
        var hash = 0L
        for (y in 0 until HASH_SIZE) {
            for (x in 0 until HASH_SIZE) {
                val left = resized.getPixel(x, y).toLuminance()
                val right = resized.getPixel(x + 1, y).toLuminance()
                hash = (hash shl 1) or if (left > right) 1L else 0L
            }
        }
        resized.recycle()
        return hash
    }

    fun hammingDistance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)

    private fun Int.toLuminance(): Int {
        val r = (this shr 16) and 0xFF
        val g = (this shr 8) and 0xFF
        val b = this and 0xFF
        return (0.299 * r + 0.587 * g + 0.114 * b).toInt()
    }
}
