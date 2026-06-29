package com.unam.gallerycleaner.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PhotoHashDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PhotoHashDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.photoHashDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_and_get_returns_stored_entity() = runTest {
        val entity = PhotoHashEntity(photoId = 1L, dHash = 0xABCDL, sharpness = 85.5)
        dao.insert(entity)

        val result = dao.get(1L)

        assertNotNull(result)
        assertEquals(0xABCDL, result!!.dHash)
        assertEquals(85.5, result.sharpness, 0.001)
    }

    @Test
    fun get_unknown_id_returns_null() = runTest {
        val result = dao.get(999L)
        assertNull(result)
    }

    @Test
    fun insert_replace_overwrites_existing_hash() = runTest {
        dao.insert(PhotoHashEntity(1L, 0L, 50.0))
        dao.insert(PhotoHashEntity(1L, 0xFFFF_FFFFL, 75.0)) // replace

        val result = dao.get(1L)

        assertEquals(0xFFFF_FFFFL, result!!.dHash)
        assertEquals(75.0, result.sharpness, 0.001)
    }

    @Test
    fun deleteStale_removes_entries_not_in_valid_ids() = runTest {
        dao.insert(PhotoHashEntity(1L, 0L, 100.0))
        dao.insert(PhotoHashEntity(2L, 0L, 100.0))
        dao.insert(PhotoHashEntity(3L, 0L, 100.0))

        dao.deleteStale(listOf(1L, 3L)) // photo 2 is stale

        assertNotNull(dao.get(1L))
        assertNull(dao.get(2L))
        assertNotNull(dao.get(3L))
    }
}
