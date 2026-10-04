package com.namilab.gallerycleaner.domain

import com.namilab.gallerycleaner.domain.model.PhotoGroup

interface GroupResultsCache {
    val cachedGroups: List<PhotoGroup>?
    val cachedTotalSavingBytes: Long
    fun save(groups: List<PhotoGroup>, totalSavingBytes: Long)
    fun clear()
}
