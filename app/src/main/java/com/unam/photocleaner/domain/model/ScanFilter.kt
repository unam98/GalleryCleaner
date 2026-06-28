package com.unam.photocleaner.domain.model

import java.util.concurrent.TimeUnit

data class ScanFilter(
    val period: ScanPeriod = ScanPeriod.ALL,
    val minSizeBytes: Long = 0L,
    val maxPhotoCount: Int? = null,
) {
    fun sinceTimestampMs(): Long? = period.days?.let {
        System.currentTimeMillis() - TimeUnit.DAYS.toMillis(it.toLong())
    }
}

enum class ScanPeriod(val label: String, val days: Int?) {
    WEEK("최근 1주", 7),
    MONTH("최근 1개월", 30),
    THREE_MONTHS("최근 3개월", 90),
    SIX_MONTHS("최근 6개월", 180),
    ALL("전체", null),
}

enum class MaxPhotoCount(val label: String, val count: Int?) {
    LATEST_100("최근 100장", 100),
    LATEST_300("최근 300장", 300),
    LATEST_500("최근 500장", 500),
    LATEST_1000("최근 1000장", 1000),
    ALL("전체", null),
}

enum class MinSize(val label: String, val bytes: Long) {
    NONE("제한 없음", 0L),
    KB500("500KB 이상", 500 * 1024L),
    MB1("1MB 이상", 1 * 1024 * 1024L),
    MB3("3MB 이상", 3 * 1024 * 1024L),
    MB5("5MB 이상", 5 * 1024 * 1024L),
}
