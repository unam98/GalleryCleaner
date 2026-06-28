package com.unam.photocleaner.domain.usecase

import android.content.Context
import android.graphics.BitmapFactory
import com.unam.photocleaner.data.local.db.PhotoHashDao
import com.unam.photocleaner.data.local.db.PhotoHashEntity
import com.unam.photocleaner.domain.model.GroupType
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.util.DHashUtils
import com.unam.photocleaner.util.ImageQualityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupPhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoHashDao: PhotoHashDao,
) {
    private val BURST_WINDOW_MS = 3_000L
    private val SIMILARITY_THRESHOLD = 10

    suspend fun execute(
        photos: List<Photo>,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ): List<PhotoGroup> = withContext(Dispatchers.Default) {
        val hashes = computeHashes(photos, onProgress)
        findGroups(photos, hashes)
    }

    private suspend fun computeHashes(
        photos: List<Photo>,
        onProgress: (Int, Int) -> Unit,
    ): Map<Long, PhotoHashEntity> {
        val result = mutableMapOf<Long, PhotoHashEntity>()
        photos.forEachIndexed { index, photo ->
            onProgress(index + 1, photos.size)
            val cached = photoHashDao.get(photo.id)
            if (cached != null) {
                result[photo.id] = cached
                return@forEachIndexed
            }
            val entity = decodeAndHash(photo) ?: return@forEachIndexed
            photoHashDao.insert(entity)
            result[photo.id] = entity
        }
        return result
    }

    private fun decodeAndHash(photo: Photo): PhotoHashEntity? {
        return try {
            val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
            val fd = context.contentResolver.openFileDescriptor(photo.uri, "r")?.fileDescriptor
                ?: return null
            val bitmap = BitmapFactory.decodeFileDescriptor(fd, null, opts) ?: return null
            val hash = DHashUtils.compute(bitmap)
            val sharpness = ImageQualityUtils.sharpness(bitmap)
            bitmap.recycle()
            PhotoHashEntity(photoId = photo.id, dHash = hash, sharpness = sharpness)
        } catch (e: Exception) {
            null
        }
    }

    private fun findGroups(
        photos: List<Photo>,
        hashes: Map<Long, PhotoHashEntity>,
    ): List<PhotoGroup> {
        val sorted = photos.sortedBy { it.dateTaken }
        val visited = BooleanArray(sorted.size)
        val groups = mutableListOf<PhotoGroup>()

        for (i in sorted.indices) {
            if (visited[i]) continue
            val anchor = sorted[i]
            val anchorHash = hashes[anchor.id]?.dHash ?: continue

            val groupPhotos = mutableListOf(anchor)
            visited[i] = true

            for (j in i + 1 until sorted.size) {
                if (visited[j]) continue
                val candidate = sorted[j]
                if (candidate.dateTaken - anchor.dateTaken > BURST_WINDOW_MS) break

                val candidateHash = hashes[candidate.id]?.dHash ?: continue
                if (DHashUtils.hammingDistance(anchorHash, candidateHash) <= SIMILARITY_THRESHOLD) {
                    groupPhotos.add(candidate)
                    visited[j] = true
                }
            }

            if (groupPhotos.size < 2) continue

            val bestId = groupPhotos.maxByOrNull { hashes[it.id]?.sharpness ?: 0.0 }?.id
                ?: groupPhotos.first().id
            val saving = groupPhotos.sumOf { it.size } - (groupPhotos.find { it.id == bestId }?.size ?: 0)

            groups.add(
                PhotoGroup(
                    id = "group_$i",
                    photos = groupPhotos,
                    bestPhotoId = bestId,
                    type = GroupType.BURST,
                    potentialSavingBytes = saving,
                )
            )
        }
        return groups.sortedByDescending { it.potentialSavingBytes }
    }
}
