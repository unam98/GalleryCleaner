package com.unam.gallerycleaner.domain.usecase

import android.content.Context
import com.unam.gallerycleaner.PhotoFactory
import com.unam.gallerycleaner.data.local.db.PhotoHashDao
import com.unam.gallerycleaner.data.local.db.PhotoHashEntity
import com.unam.gallerycleaner.domain.model.GroupType
import com.unam.gallerycleaner.util.EmbeddingExtractor
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GroupBurstLogicTest {

    private val context = mockk<Context>(relaxed = true)
    private val photoHashDao = mockk<PhotoHashDao>()
    private val embeddingExtractor = mockk<EmbeddingExtractor>()
    private lateinit var useCase: GroupPhotosUseCase

    // computeHashes()는 이제 getAll() 단일 쿼리 + insertAll() 배치 insert 사용
    private val hashEntities = mutableListOf<PhotoHashEntity>()

    @Before
    fun setUp() {
        hashEntities.clear()
        useCase = GroupPhotosUseCase(context, photoHashDao, embeddingExtractor)
        coEvery { embeddingExtractor.initialize() } returns false
        coEvery { photoHashDao.getAll() } answers { hashEntities.toList() }
        coEvery { photoHashDao.insertAll(any()) } just runs
    }

    private fun stubHash(photoId: Long, dHash: Long = 0L, sharpness: Double = 100.0) {
        hashEntities.add(PhotoHashEntity(photoId, dHash, sharpness))
    }

    @Test
    fun `three photos within burst window with same hash form one group`() = runTest {
        val t0 = 1_000_000L
        val photos = listOf(
            PhotoFactory.photo(1L, dateTaken = t0),
            PhotoFactory.photo(2L, dateTaken = t0 + 1_000),
            PhotoFactory.photo(3L, dateTaken = t0 + 2_000),
        )
        photos.forEach { stubHash(it.id, dHash = 0L) }

        val groups = useCase.execute(photos)

        assertEquals(1, groups.size)
        assertEquals(3, groups[0].photos.size)
        assertEquals(GroupType.BURST, groups[0].type)
    }

    @Test
    fun `photos outside 5s burst window are not grouped`() = runTest {
        val t0 = 1_000_000L
        val photos = listOf(
            PhotoFactory.photo(1L, dateTaken = t0),
            PhotoFactory.photo(2L, dateTaken = t0 + 6_000), // > 5s gap → not burst
        )
        photos.forEach { stubHash(it.id, dHash = 0L) }

        val groups = useCase.execute(photos)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `photos within time window but different hash are not grouped`() = runTest {
        val t0 = 1_000_000L
        val photos = listOf(
            PhotoFactory.photo(1L, dateTaken = t0),
            PhotoFactory.photo(2L, dateTaken = t0 + 1_000),
        )
        stubHash(1L, dHash = 0L)
        stubHash(2L, dHash = -1L) // hammingDistance(0L, -1L) = 64 > 10

        val groups = useCase.execute(photos)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `best photo in burst group is the sharpest one`() = runTest {
        val t0 = 1_000_000L
        val photos = listOf(
            PhotoFactory.photo(1L, dateTaken = t0, size = 500_000L),
            PhotoFactory.photo(2L, dateTaken = t0 + 1_000, size = 600_000L),
        )
        stubHash(1L, dHash = 0L, sharpness = 80.0)
        stubHash(2L, dHash = 0L, sharpness = 120.0)

        val groups = useCase.execute(photos)

        assertEquals(1, groups.size)
        assertEquals(2L, groups[0].bestPhotoId) // photo 2 is sharper
    }

    @Test
    fun `photos with no cached hash and no real context produce no groups`() = runTest {
        val t0 = 1_000_000L
        val photos = listOf(
            PhotoFactory.photo(1L, dateTaken = t0),
            PhotoFactory.photo(2L, dateTaken = t0 + 500),
        )
        // getAll() returns empty → all photos hit decodeAndHash → context (relaxed mock) returns null

        val groups = useCase.execute(photos)

        assertTrue(groups.isEmpty())
    }

    @Test
    fun `two independent burst groups from two time clusters`() = runTest {
        val t0 = 1_000_000L
        val t1 = t0 + 60_000L // 60s later, different cluster
        val photos = listOf(
            PhotoFactory.photo(1L, dateTaken = t0),
            PhotoFactory.photo(2L, dateTaken = t0 + 1_000),
            PhotoFactory.photo(3L, dateTaken = t1),
            PhotoFactory.photo(4L, dateTaken = t1 + 1_000),
        )
        photos.forEach { stubHash(it.id, dHash = 0L) }

        val groups = useCase.execute(photos)

        assertEquals(2, groups.size)
        assertTrue(groups.all { it.type == GroupType.BURST })
    }
}
