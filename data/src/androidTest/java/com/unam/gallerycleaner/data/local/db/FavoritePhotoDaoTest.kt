package com.unam.gallerycleaner.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoritePhotoDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FavoritePhotoDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.favoritePhotoDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insert_and_observeAll_emits_inserted_id() = runTest {
        dao.insert(FavoritePhotoEntity(42L))

        dao.observeAll().test {
            val ids = awaitItem()
            assertTrue(ids.contains(42L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_removes_photo_from_favorites() = runTest {
        dao.insert(FavoritePhotoEntity(10L))
        dao.insert(FavoritePhotoEntity(20L))
        dao.delete(10L)

        dao.observeAll().test {
            val ids = awaitItem()
            assertTrue(!ids.contains(10L))
            assertTrue(ids.contains(20L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAll_emits_updated_list_after_insert() = runTest {
        dao.observeAll().test {
            val empty = awaitItem()
            assertTrue(empty.isEmpty())

            dao.insert(FavoritePhotoEntity(5L))
            val updated = awaitItem()
            assertEquals(listOf(5L), updated)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun insert_duplicate_does_not_throw() = runTest {
        dao.insert(FavoritePhotoEntity(99L))
        dao.insert(FavoritePhotoEntity(99L)) // REPLACE strategy, no crash

        dao.observeAll().test {
            val ids = awaitItem()
            assertEquals(1, ids.size)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
