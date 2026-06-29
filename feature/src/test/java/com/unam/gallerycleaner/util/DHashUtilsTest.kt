package com.unam.gallerycleaner.util

import org.junit.Assert.assertEquals
import org.junit.Test

class DHashUtilsTest {

    @Test
    fun `hammingDistance - identical hashes returns 0`() {
        assertEquals(0, DHashUtils.hammingDistance(0x1234_5678_ABCDL, 0x1234_5678_ABCDL))
    }

    @Test
    fun `hammingDistance - completely different hashes returns 64`() {
        assertEquals(64, DHashUtils.hammingDistance(0L, -1L))
    }

    @Test
    fun `hammingDistance - single bit difference returns 1`() {
        assertEquals(1, DHashUtils.hammingDistance(0b0000L, 0b0001L))
    }

    @Test
    fun `hammingDistance - is symmetric`() {
        val a = 0x1234_5678_90ABL
        val b = 0x0FED_CBA0_9876L
        assertEquals(
            DHashUtils.hammingDistance(a, b),
            DHashUtils.hammingDistance(b, a),
        )
    }

    @Test
    fun `hammingDistance - single bit flip gives distance 1`() {
        val base = 0b1111_1111_0000_0000L
        val similar = base xor 1L
        assertEquals(1, DHashUtils.hammingDistance(base, similar))
    }
}
