package com.namilab.gallerycleaner.presentation

import com.namilab.gallerycleaner.domain.model.Photo

data class TournamentState(
    val groupId: String,
    val bracket: List<Photo>,
    val nextRound: List<Photo>,
    val pairIndex: Int,
    val roundNumber: Int,
    val totalPhotos: Int,
    /** 여러 그룹을 순차 진행하는 큐 모드일 때 현재 몇 번째 그룹인지 (1-based). 단일 그룹 실행 시 1/1. */
    val queuePosition: Int = 1,
    val queueTotal: Int = 1,
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
