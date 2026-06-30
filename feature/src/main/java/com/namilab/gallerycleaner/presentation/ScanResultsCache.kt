package com.namilab.gallerycleaner.presentation

import com.namilab.gallerycleaner.domain.GroupResultsCache
import com.namilab.gallerycleaner.domain.model.PhotoGroup
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanResultsCache @Inject constructor() : GroupResultsCache {
    @Volatile override var cachedGroups: List<PhotoGroup>? = null
    @Volatile override var cachedTotalSavingBytes: Long = 0L

    override fun save(groups: List<PhotoGroup>, totalSavingBytes: Long) {
        cachedGroups = groups
        cachedTotalSavingBytes = totalSavingBytes
    }

    override fun clear() {
        cachedGroups = null
        cachedTotalSavingBytes = 0L
    }
}
