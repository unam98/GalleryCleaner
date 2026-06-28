package com.unam.photocleaner.domain.model

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateTaken: Long,
    val size: Long,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val duration: Long = 0L,  // 0 = 사진, >0 = 동영상 길이(ms)
) {
    val isVideo: Boolean get() = duration > 0L
}
