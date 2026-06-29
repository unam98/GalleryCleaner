package com.unam.gallerycleaner.presentation

import com.unam.gallerycleaner.domain.model.Photo

data class TournamentState(
    val groupId: String,
    val bracket: List<Photo>,
    val nextRound: List<Photo>,
    val pairIndex: Int,
    val roundNumber: Int,
    val totalPhotos: Int,
) {
    val left: Photo? get() = bracket.getOrNull(pairIndex * 2)
    val right: Photo? get() = bracket.getOrNull(pairIndex * 2 + 1)

    val isComplete: Boolean get() = bracket.size == 1 && nextRound.isEmpty()
    val winner: Photo? get() = bracket.firstOrNull().takeIf { isComplete }

    val totalRounds: Int get() {
        var n = totalPhotos
        var rounds = 0
        while (n > 1) { n = (n + 1) / 2; rounds++ }
        return rounds
    }
}
