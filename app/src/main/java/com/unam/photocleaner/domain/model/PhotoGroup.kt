package com.unam.photocleaner.domain.model

data class PhotoGroup(
    val id: String,
    val photos: List<Photo>,
    val bestPhotoId: Long,
    val type: GroupType,
    val potentialSavingBytes: Long,
)

enum class GroupType {
    BURST,
    SIMILAR,
}
