package com.unam.gallerycleaner.presentation

import com.unam.gallerycleaner.domain.model.PhotoGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanResultsCache @Inject constructor() {
    @Volatile var cachedGroups: List<PhotoGroup>? = null
    @Volatile var cachedTotalSavingBytes: Long = 0L

    fun save(groups: List<PhotoGroup>, totalSavingBytes: Long) {
        cachedGroups = groups
        cachedTotalSavingBytes = totalSavingBytes
    }

    fun clear() {
        cachedGroups = null
        cachedTotalSavingBytes = 0L
    }
}
