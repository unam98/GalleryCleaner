package com.unam.photocleaner.domain.usecase

import android.content.Context
import android.graphics.BitmapFactory
import com.unam.photocleaner.data.local.db.PhotoHashDao
import com.unam.photocleaner.data.local.db.PhotoHashEntity
import com.unam.photocleaner.domain.model.GroupType
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.util.DHashUtils
import com.unam.photocleaner.util.EmbeddingExtractor
import com.unam.photocleaner.util.ImageQualityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupPhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoHashDao: PhotoHashDao,
    private val embeddingExtractor: EmbeddingExtractor,
) {
    private val BURST_WINDOW_MS = 3_000L
    private val BURST_HASH_THRESHOLD = 10
    private val SIMILAR_WINDOW_MS = 2 * 60 * 60 * 1000L  // 2시간
    private val SIMILAR_THRESHOLD = 0.88f

    suspend fun execute(
        photos: List<Photo>,
        onProgress: (current: Int, total: Int, label: String) -> Unit = { _, _, _ -> },
    ): List<PhotoGroup> = withContext(Dispatchers.Default) {
        // Phase 1: dHash로 버스트 그룹 탐색
        val hashes = computeHashes(photos) { curr, total ->
            onProgress(curr, total, "해시 분석 중")
        }
        val burstGroups = findBurstGroups(photos, hashes)

        // Phase 2: TFLite 임베딩으로 유사 사진 탐색 (버스트에서 제외된 사진 대상)
        val burstIds = burstGroups.flatMap { g -> g.photos.map { it.id } }.toHashSet()
        val ungrouped = photos.filter { it.id !in burstIds }
        val similarGroups = findSimilarGroups(ungrouped, hashes) { curr, total ->
            onProgress(curr, total, "AI 유사 사진 탐색 중")
        }

        (burstGroups + similarGroups).sortedByDescending { it.potentialSavingBytes }
    }

    // ── Phase 1 ──────────────────────────────────────────────────────────────

    private suspend fun computeHashes(
        photos: List<Photo>,
        onProgress: (Int, Int) -> Unit,
    ): Map<Long, PhotoHashEntity> {
        val result = mutableMapOf<Long, PhotoHashEntity>()
        photos.forEachIndexed { index, photo ->
            onProgress(index + 1, photos.size)
            val cached = photoHashDao.get(photo.id)
            if (cached != null) { result[photo.id] = cached; return@forEachIndexed }
            val entity = decodeAndHash(photo) ?: return@forEachIndexed
            photoHashDao.insert(entity)
            result[photo.id] = entity
        }
        return result
    }

    private fun decodeAndHash(photo: Photo): PhotoHashEntity? = try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val fd = context.contentResolver.openFileDescriptor(photo.uri, "r")?.fileDescriptor
            ?: return null
        val bitmap = BitmapFactory.decodeFileDescriptor(fd, null, opts) ?: return null
        val hash = DHashUtils.compute(bitmap)
        val sharpness = ImageQualityUtils.sharpness(bitmap)
        bitmap.recycle()
        PhotoHashEntity(photoId = photo.id, dHash = hash, sharpness = sharpness)
    } catch (e: Exception) { null }

    private fun findBurstGroups(
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
                if (DHashUtils.hammingDistance(anchorHash, candidateHash) <= BURST_HASH_THRESHOLD) {
                    groupPhotos.add(candidate); visited[j] = true
                }
            }
            if (groupPhotos.size < 2) continue
            groups.add(makeGroup("burst_$i", groupPhotos, hashes, GroupType.BURST))
        }
        return groups
    }

    // ── Phase 2 ──────────────────────────────────────────────────────────────

    private suspend fun findSimilarGroups(
        photos: List<Photo>,
        hashes: Map<Long, PhotoHashEntity>,
        onProgress: (Int, Int) -> Unit,
    ): List<PhotoGroup> {
        if (photos.isEmpty()) return emptyList()
        if (!embeddingExtractor.initialize()) return emptyList()

        val sorted = photos.sortedBy { it.dateTaken }
        val embeddings = mutableMapOf<Long, FloatArray>()

        sorted.forEachIndexed { index, photo ->
            onProgress(index + 1, sorted.size)
            val entity = hashes[photo.id]
            val cached = entity?.embedding
            if (cached != null) {
                embeddings[photo.id] = cached.toFloatArray()
                return@forEachIndexed
            }
            val emb = computeEmbedding(photo) ?: return@forEachIndexed
            embeddings[photo.id] = emb
            if (entity != null) {
                photoHashDao.insert(entity.copy(embedding = emb.toByteArray()))
            }
        }

        val visited = BooleanArray(sorted.size)
        val groups = mutableListOf<PhotoGroup>()

        for (i in sorted.indices) {
            if (visited[i]) continue
            val anchor = sorted[i]
            val anchorEmb = embeddings[anchor.id] ?: continue
            val groupPhotos = mutableListOf(anchor)
            visited[i] = true

            for (j in i + 1 until sorted.size) {
                if (visited[j]) continue
                val candidate = sorted[j]
                if (candidate.dateTaken - anchor.dateTaken > SIMILAR_WINDOW_MS) break
                val candidateEmb = embeddings[candidate.id] ?: continue
                if (embeddingExtractor.cosineSimilarity(anchorEmb, candidateEmb) >= SIMILAR_THRESHOLD) {
                    groupPhotos.add(candidate); visited[j] = true
                }
            }
            if (groupPhotos.size < 2) continue
            groups.add(makeGroup("similar_$i", groupPhotos, hashes, GroupType.SIMILAR))
        }
        return groups
    }

    private fun computeEmbedding(photo: Photo): FloatArray? = try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
        val fd = context.contentResolver.openFileDescriptor(photo.uri, "r")?.fileDescriptor
            ?: return null
        val bitmap = BitmapFactory.decodeFileDescriptor(fd, null, opts) ?: return null
        val emb = embeddingExtractor.extract(bitmap)
        bitmap.recycle()
        emb
    } catch (e: Exception) { null }

    // ── 직렬화 헬퍼 ──────────────────────────────────────────────────────────

    private fun FloatArray.toByteArray(): ByteArray {
        val buf = ByteBuffer.allocate(size * 4).order(ByteOrder.LITTLE_ENDIAN)
        buf.asFloatBuffer().put(this)
        return buf.array()
    }

    private fun ByteArray.toFloatArray(): FloatArray {
        val buf = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(buf.limit()).also { buf.get(it) }
    }

    // ── 공통 ─────────────────────────────────────────────────────────────────

    private fun makeGroup(
        id: String,
        photos: List<Photo>,
        hashes: Map<Long, PhotoHashEntity>,
        type: GroupType,
    ): PhotoGroup {
        val bestId = photos.maxByOrNull { hashes[it.id]?.sharpness ?: 0.0 }?.id
            ?: photos.first().id
        val saving = photos.sumOf { it.size } - (photos.find { it.id == bestId }?.size ?: 0L)
        return PhotoGroup(id = id, photos = photos, bestPhotoId = bestId, type = type,
            potentialSavingBytes = saving)
    }
}
