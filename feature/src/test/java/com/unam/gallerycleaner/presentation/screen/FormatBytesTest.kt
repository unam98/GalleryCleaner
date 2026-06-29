package com.unam.gallerycleaner.presentation.screen

import com.unam.gallerycleaner.domain.util.formatBytes
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatBytesTest {

    @Test
    fun `bytes under 1KB returns B suffix`() {
        assertEquals("512 B", formatBytes(512L))
    }

    @Test
    fun `1KB returns KB suffix`() {
        assertEquals("1.0 KB", formatBytes(1_024L))
    }

    @Test
    fun `1MB returns MB suffix`() {
        assertEquals("1.0 MB", formatBytes(1_048_576L))
    }

    @Test
    fun `1GB returns GB suffix`() {
        assertEquals("1.0 GB", formatBytes(1_073_741_824L))
    }

    @Test
    fun `1500KB rounds to MB`() {
        assertEquals("1.5 MB", formatBytes(1_572_864L))
    }

    @Test
    fun `0 bytes returns 0 B`() {
        assertEquals("0 B", formatBytes(0L))
    }
}
