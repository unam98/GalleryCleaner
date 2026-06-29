package com.namilab.gallerycleaner.presentation

import com.namilab.gallerycleaner.domain.model.PhotoGroup

enum class GroupSortOrder {
    SAVING_DESC,
    SAVING_ASC,
    COUNT_DESC,
    COUNT_ASC,
    DATE_OLD,
    DATE_NEW;

    fun apply(groups: List<PhotoGroup>): List<PhotoGroup> = when (this) {
        SAVING_DESC -> groups.sortedByDescending { it.potentialSavingBytes }
        SAVING_ASC  -> groups.sortedBy { it.potentialSavingBytes }
        COUNT_DESC  -> groups.sortedByDescending { it.photos.size }
        COUNT_ASC   -> groups.sortedBy { it.photos.size }
        DATE_OLD    -> groups.sortedBy { it.photos.minOf { p -> p.dateTaken } }
        DATE_NEW    -> groups.sortedByDescending { it.photos.maxOf { p -> p.dateTaken } }
    }

    fun toggle(): GroupSortOrder = when (this) {
        SAVING_DESC -> SAVING_ASC;  SAVING_ASC -> SAVING_DESC
        COUNT_DESC  -> COUNT_ASC;   COUNT_ASC  -> COUNT_DESC
        DATE_OLD    -> DATE_NEW;    DATE_NEW   -> DATE_OLD
    }
}
