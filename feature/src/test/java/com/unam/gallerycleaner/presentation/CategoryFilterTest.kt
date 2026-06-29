package com.unam.gallerycleaner.presentation

import com.unam.gallerycleaner.PhotoFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryFilterTest {

    private val dogPhoto = PhotoFactory.photo(id = 1L)
    private val personPhoto = PhotoFactory.photo(id = 2L)
    private val dogAndPersonPhoto = PhotoFactory.photo(id = 3L)
    private val foodPhoto = PhotoFactory.photo(id = 4L)

    private val photoLabels = mapOf(
        1L to listOf("Dog", "Grass", "Outdoor"),
        2L to listOf("Person", "Face", "Smile"),
        3L to listOf("Dog", "Person", "Outdoor"),
        4L to listOf("Food", "Meal", "Dish"),
    )

    private val groups = listOf(
        PhotoFactory.group(id = "g1", photos = listOf(dogPhoto, PhotoFactory.photo(5L))),
        PhotoFactory.group(id = "g2", photos = listOf(personPhoto, PhotoFactory.photo(6L))),
        PhotoFactory.group(id = "g3", photos = listOf(dogAndPersonPhoto, PhotoFactory.photo(7L))),
        PhotoFactory.group(id = "g4", photos = listOf(foodPhoto, PhotoFactory.photo(8L))),
    )

    @Test
    fun `no categories selected returns all groups`() {
        val result = MainViewModel.applyFilter(groups, photoLabels, emptySet())
        assertEquals(4, result.size)
    }

    @Test
    fun `single category 동물 returns groups containing dog label`() {
        val result = MainViewModel.applyFilter(groups, photoLabels, setOf("동물"))
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == "g1" })
        assertTrue(result.any { it.id == "g3" })
    }

    @Test
    fun `AND filter 동물+인물 returns only group with both labels`() {
        val result = MainViewModel.applyFilter(groups, photoLabels, setOf("동물", "인물"))
        assertEquals(1, result.size)
        assertEquals("g3", result.first().id)
    }

    @Test
    fun `category with no matches returns empty list`() {
        val result = MainViewModel.applyFilter(groups, photoLabels, setOf("풍경"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `AND filter impossible combination returns empty`() {
        val result = MainViewModel.applyFilter(groups, photoLabels, setOf("동물", "음식"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `filter matches labels case-insensitively`() {
        val labelsLower = mapOf(
            1L to listOf("dog", "grass"),
            2L to listOf("dog", "grass"),
        )
        val twoPhotoGroup = listOf(PhotoFactory.group("g1", listOf(dogPhoto, PhotoFactory.photo(2L))))
        val result = MainViewModel.applyFilter(twoPhotoGroup, labelsLower, setOf("동물"))
        assertEquals(1, result.size)
    }

    @Test
    fun `음식 category matches food group only`() {
        val result = MainViewModel.applyFilter(groups, photoLabels, setOf("음식"))
        assertEquals(1, result.size)
        assertEquals("g4", result.first().id)
    }
}
