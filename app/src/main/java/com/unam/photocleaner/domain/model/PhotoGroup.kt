package com.unam.photocleaner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PhotoGroup(
    val id: String,
    val photos: List<Photo>,
    val bestPhotoId: Long,
    val type: GroupType,
    val potentialSavingBytes: Long,
) : Parcelable

enum class GroupType {
    BURST,
    SIMILAR,
    VIDEO_DUPLICATE,
    SHORT_VIDEO,
}
