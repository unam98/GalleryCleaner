package com.unam.gallerycleaner.domain.usecase

import com.unam.gallerycleaner.domain.model.GroupType
import com.unam.gallerycleaner.domain.model.Photo
import com.unam.gallerycleaner.domain.model.PhotoGroup
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class GroupVideosUseCase @Inject constructor() {

    fun execute(videos: List<Photo>): List<PhotoGroup> {
        val groups = mutableListOf<PhotoGroup>()
        groups += findShortVideoGroup(videos)
        groups += findDuplicateGroups(videos.filter { it.duration > SHORT_THRESHOLD_MS })
        return groups.sortedByDescending { it.potentialSavingBytes }
    }

    // 10초 이하 짧은 영상 → 전부 하나의 그룹으로
    private fun findShortVideoGroup(videos: List<Photo>): List<PhotoGroup> {
        val shorts = videos.filter { it.duration in 1..SHORT_THRESHOLD_MS }
        if (shorts.size < 2) return emptyList()
        val bestId = shorts.maxByOrNull { it.duration }?.id ?: shorts.first().id
        val saving = shorts.sumOf { it.size } - (shorts.find { it.id == bestId }?.size ?: 0L)
        return listOf(PhotoGroup(
            id = "short_videos",
            photos = shorts,
            bestPhotoId = bestId,
            type = GroupType.SHORT_VIDEO,
            potentialSavingBytes = saving,
        ))
    }

    // 같은 시간대(1시간 내)에 재생 길이 차이 2초 이내인 영상 → 중복으로 판단
    private fun findDuplicateGroups(videos: List<Photo>): List<PhotoGroup> {
        val sorted = videos.sortedBy { it.dateTaken }
        val visited = BooleanArray(sorted.size)
        val groups = mutableListOf<PhotoGroup>()

        for (i in sorted.indices) {
            if (visited[i]) continue
            val anchor = sorted[i]
            val group = mutableListOf(anchor)
            visited[i] = true

            for (j in i + 1 until sorted.size) {
                if (visited[j]) continue
                val candidate = sorted[j]
                if (candidate.dateTaken - anchor.dateTaken > DUPLICATE_WINDOW_MS) break
                if (abs(anchor.duration - candidate.duration) <= DURATION_TOLERANCE_MS) {
                    group.add(candidate)
                    visited[j] = true
                }
            }
            if (group.size < 2) continue
            val bestId = group.maxByOrNull { it.size }?.id ?: group.first().id
            val saving = group.sumOf { it.size } - (group.find { it.id == bestId }?.size ?: 0L)
            groups.add(PhotoGroup(
                id = "video_dup_$i",
                photos = group,
                bestPhotoId = bestId,
                type = GroupType.VIDEO_DUPLICATE,
                potentialSavingBytes = saving,
            ))
        }
        return groups
    }

    companion object {
        const val SHORT_THRESHOLD_MS = 10_000L      // 10초 이하 = 짧은 영상
        const val DUPLICATE_WINDOW_MS = 3_600_000L  // 1시간 내
        const val DURATION_TOLERANCE_MS = 2_000L    // 재생 길이 차이 ±2초 이내 = 중복
    }
}
