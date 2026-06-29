package com.unam.gallerycleaner.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.unam.gallerycleaner.data.local.db.PhotoHashDao
import com.unam.gallerycleaner.data.local.db.PhotoHashEntity
import com.unam.gallerycleaner.domain.model.GroupType
import com.unam.gallerycleaner.domain.model.Photo
import com.unam.gallerycleaner.domain.model.PhotoGroup
import com.unam.gallerycleaner.util.DHashUtils
import com.unam.gallerycleaner.util.EmbeddingExtractor
import com.unam.gallerycleaner.util.ImageQualityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroupPhotosUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val photoHashDao: PhotoHashDao,
    private val embeddingExtractor: EmbeddingExtractor,
) {
    private val EXACT_MIN_GAP_MS = 60 * 60 * 1000L
    private val BURST_WINDOW_MS = 5_000L
    private val BURST_HASH_THRESHOLD = 10
    private val SIMILAR_WINDOW_MS = 2 * 60 * 60 * 1000L
    private val SIMILAR_THRESHOLD = 0.85f
    private val IO_CONCURRENCY = 4
    // TFLite 호출 전 dHash 유사도로 후보 사전 필터링 — 이 거리 이하인 이웃이 없으면 TFLite 건너뜀
    private val DHASH_PREFILTER_THRESHOLD = 20

    suspend fun execute(
        photos: List<Photo>,
        onProgress: (current: Int, total: Int, label: String) -> Unit = { _, _, _ -> },
    ): List<PhotoGroup> = withContext(Dispatchers.Default) {
        // 해시 단계: 전체 N장 progress (0..N)
        val hashes = computeHashes(photos) { curr, total ->
            onProgress(curr, total, "")
        }

        val exactGroups = findExactDuplicates(photos, hashes)
        val exactIds = exactGroups.flatMap { it.photos.map { p -> p.id } }.toHashSet()

        val nonExact = photos.filter { it.id !in exactIds }
        val burstGroups = findBurstGroups(nonExact, hashes)

        val burstIds = burstGroups.flatMap { g -> g.photos.map { it.id } }.toHashSet()
        val ungrouped = nonExact.filter { it.id !in burstIds }

        // 임베딩 단계: progress를 N 이후로 이어받아 N+M 까지 단일 흐름으로 표시
        val grandTotal = photos.size + ungrouped.size
        val similarGroups = findSimilarGroups(ungrouped, hashes) { embCurr, _ ->
            onProgress(photos.size + embCurr, grandTotal, "")
        }

        (exactGroups + burstGroups + similarGroups).sortedByDescending { it.potentialSavingBytes }
    }

    // ── Phase 0: O(n) HashMap bucketing ──────────────────────────────────────
    // 기존 O(n²) pairwise → 동일 dHash 버킷 단위 비교로 변경
    private fun findExactDuplicates(
        photos: List<Photo>,
        hashes: Map<Long, PhotoHashEntity>,
    ): List<PhotoGroup> {
        val buckets = HashMap<Long, MutableList<Photo>>()
        for (photo in photos) {
            val hash = hashes[photo.id]?.dHash ?: continue
            buckets.getOrPut(hash) { mutableListOf() }.add(photo)
        }

        val groups = mutableListOf<PhotoGroup>()
        val globalVisited = hashSetOf<Long>()

        for ((_, bucket) in buckets) {
            if (bucket.size < 2) continue
            val sorted = bucket.sortedBy { it.dateTaken }

            for (anchor in sorted) {
                if (anchor.id in globalVisited) continue
                val groupPhotos = mutableListOf(anchor)

                for (candidate in sorted) {
                    if (candidate.id == anchor.id || candidate.id in globalVisited) continue
                    // 1시간 이상 차이 = burst가 아닌 재다운로드/복사본
                    if (kotlin.math.abs(candidate.dateTaken - anchor.dateTaken) < EXACT_MIN_GAP_MS) continue
                    groupPhotos.add(candidate)
                }

                if (groupPhotos.size >= 2) {
                    groups.add(makeGroup("exact_${anchor.id}", groupPhotos, hashes, GroupType.BURST))
                    globalVisited.addAll(groupPhotos.map { it.id })
                }
            }
        }
        return groups
    }

    // ── Hash 계산: 벌크 캐시 로드 + 병렬 IO decode + 배치 insert ────────────
    private suspend fun computeHashes(
        photos: List<Photo>,
        onProgress: (Int, Int) -> Unit,
    ): Map<Long, PhotoHashEntity> = coroutineScope {
        // 단일 쿼리로 전체 캐시 로드 (N번 DB 왕복 → 1번)
        val cached = photoHashDao.getAll().associateByTo(ConcurrentHashMap()) { it.photoId }
        val uncached = photos.filter { !cached.containsKey(it.id) }
        onProgress(photos.size - uncached.size, photos.size)

        val progress = AtomicInteger(photos.size - uncached.size)
        val newEntities = Collections.synchronizedList(mutableListOf<PhotoHashEntity>())
        val semaphore = Semaphore(IO_CONCURRENCY)

        uncached.map { photo ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val entity = decodeAndHash(photo)
                    if (entity != null) {
                        cached[photo.id] = entity
                        newEntities.add(entity)
                    }
                    onProgress(progress.incrementAndGet(), photos.size)
                }
            }
        }.awaitAll()

        // 배치 insert (N 트랜잭션 → 1 트랜잭션)
        if (newEntities.isNotEmpty()) photoHashDao.insertAll(newEntities)
        cached
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

    // ── Phase 1 ──────────────────────────────────────────────────────────────

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

    // ── Phase 2: 병렬 decode → 직렬 TFLite → 배치 저장 ─────────────────────
    private suspend fun findSimilarGroups(
        photos: List<Photo>,
        hashes: Map<Long, PhotoHashEntity>,
        onProgress: (Int, Int) -> Unit,
    ): List<PhotoGroup> = coroutineScope {
        if (photos.isEmpty()) return@coroutineScope emptyList()
        if (!embeddingExtractor.initialize()) return@coroutineScope emptyList()

        val sorted = photos.sortedBy { it.dateTaken }
        val embeddings = ConcurrentHashMap<Long, FloatArray>()

        // dHash 사전 필터: TFLite 후보만 선별 (O(n×w), w=시간창 내 이웃 수)
        val embeddingCandidateIds = prefilterSimilarCandidates(sorted, hashes)

        // 캐시된 임베딩은 무조건 포함, 미캐시는 후보인 경우에만 TFLite 실행
        val uncachedPhotos = mutableListOf<Photo>()
        sorted.forEach { photo ->
            val cached = hashes[photo.id]?.embedding?.toFloatArray()
            when {
                cached != null -> embeddings[photo.id] = cached
                photo.id in embeddingCandidateIds -> uncachedPhotos.add(photo)
                // else: dHash 이웃 없음 → TFLite 건너뜀
            }
        }
        onProgress(sorted.size - uncachedPhotos.size, sorted.size)

        // Step 1: 병렬 bitmap decode (IO bound) — TFLite 대기 없이 미리 로드
        val semaphore = Semaphore(IO_CONCURRENCY)
        val decodedBitmaps: List<Pair<Photo, Bitmap?>> = uncachedPhotos.map { photo ->
            async(Dispatchers.IO) {
                semaphore.withPermit { photo to decodeBitmapForEmbedding(photo) }
            }
        }.awaitAll()

        // Step 2: 직렬 TFLite 추론 (Interpreter는 thread-safe 아님)
        val newEmbeddings = mutableListOf<Pair<Long, FloatArray>>()
        decodedBitmaps.forEachIndexed { index, (photo, bitmap) ->
            onProgress(sorted.size - uncachedPhotos.size + index + 1, sorted.size)
            bitmap ?: return@forEachIndexed
            val emb = embeddingExtractor.extract(bitmap)
            bitmap.recycle()
            if (emb != null) {
                embeddings[photo.id] = emb
                newEmbeddings.add(photo.id to emb)
            }
        }

        // Step 3: 배치 저장
        if (newEmbeddings.isNotEmpty()) {
            photoHashDao.insertAll(
                newEmbeddings.mapNotNull { (id, emb) -> hashes[id]?.copy(embedding = emb.toByteArray()) }
            )
        }

        // Step 4: 시간 창 내 pairwise 유사도 비교
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
        groups
    }

    // 시간 창 내에 dHash 거리 ≤ DHASH_PREFILTER_THRESHOLD 인 이웃이 하나라도 있는 사진만 TFLite 후보로 선별
    // sorted(dateTaken 오름차순) 전제 — 시간창 벗어나면 break로 조기 종료
    private fun prefilterSimilarCandidates(
        sorted: List<Photo>,
        hashes: Map<Long, PhotoHashEntity>,
    ): Set<Long> {
        val candidates = hashSetOf<Long>()
        for (i in sorted.indices) {
            val iHash = hashes[sorted[i].id]?.dHash ?: continue
            for (j in i + 1 until sorted.size) {
                val other = sorted[j]
                if (other.dateTaken - sorted[i].dateTaken > SIMILAR_WINDOW_MS) break
                val jHash = hashes[other.id]?.dHash ?: continue
                if (DHashUtils.hammingDistance(iHash, jHash) <= DHASH_PREFILTER_THRESHOLD) {
                    candidates.add(sorted[i].id)
                    candidates.add(other.id)
                }
            }
        }
        return candidates
    }

    private fun decodeBitmapForEmbedding(photo: Photo): Bitmap? = try {
        val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
        val fd = context.contentResolver.openFileDescriptor(photo.uri, "r")?.fileDescriptor
            ?: return null
        BitmapFactory.decodeFileDescriptor(fd, null, opts)
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
