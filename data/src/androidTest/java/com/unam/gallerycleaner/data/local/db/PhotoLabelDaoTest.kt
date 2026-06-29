package com.unam.gallerycleaner.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoLabelDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PhotoLabelDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.photoLabelDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun upsert_and_getByPhotoIds_returns_stored_labels() = runTest {
        dao.upsertAll(listOf(
            PhotoLabelEntity(1L, "Dog,Grass,Outdoor"),
            PhotoLabelEntity(2L, "Person,Face"),
        ))

        val result = dao.getByPhotoIds(listOf(1L, 2L))

        assertEquals(2, result.size)
        assertEquals("Dog,Grass,Outdoor", result.find { it.photoId == 1L }?.labels)
    }

    @Test
    fun getByPhotoIds_for_unknown_ids_returns_empty() = runTest {
        val result = dao.getByPhotoIds(listOf(99L, 100L))
        assertTrue(result.isEmpty())
    }

    @Test
    fun upsert_overwrites_existing_labels() = runTest {
        dao.upsertAll(listOf(PhotoLabelEntity(1L, "Dog")))
        dao.upsertAll(listOf(PhotoLabelEntity(1L, "Dog,Cat"))) // update

        val result = dao.getByPhotoIds(listOf(1L))

        assertEquals(1, result.size)
        assertEquals("Dog,Cat", result[0].labels)
    }

    @Test
    fun partial_id_query_returns_only_matching_rows() = runTest {
        dao.upsertAll(listOf(
            PhotoLabelEntity(1L, "Dog"),
            PhotoLabelEntity(2L, "Cat"),
            PhotoLabelEntity(3L, "Fish"),
        ))

        val result = dao.getByPhotoIds(listOf(1L, 3L))

        assertEquals(2, result.size)
        assertTrue(result.none { it.photoId == 2L })
    }

    @Test
    fun upsert_large_batch_all_stored() = runTest {
        val entities = (1L..100L).map { PhotoLabelEntity(it, "Label_$it") }
        dao.upsertAll(entities)

        val result = dao.getByPhotoIds((1L..100L).toList())

        assertEquals(100, result.size)
    }
}
