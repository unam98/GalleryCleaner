package com.namilab.gallerycleaner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScanFilter(
    val period: ScanPeriod = ScanPeriod.ALL,
    val minSizeBytes: Long = 0L,
    val maxVideoDurationMs: Long = Long.MAX_VALUE,
    val customSinceMs: Long? = null,
    val mediaType: MediaType = MediaType.ALL,
) : Parcelable {
    fun sinceTimestampMs(): Long? = customSinceMs ?: period.sinceMs()
    fun untilTimestampMs(): Long? = period.untilMs()
}

private val DAY_MS = 86_400_000L

enum class ScanPeriod(val label: String, val recentDays: Int?, val olderThanDays: Int?) {
    WEEK("최근 1주", 7, null),
    MONTH("최근 1개월", 30, null),
    THREE_MONTHS("최근 3개월", 90, null),
    SIX_MONTHS("최근 6개월", 180, null),
    ONE_YEAR("최근 1년", 365, null),
    TWO_YEARS("최근 2년", 730, null),
    THREE_YEARS_PLUS("3년 이상 된", null, 3 * 365),
    FIVE_YEARS_PLUS("5년 이상 된", null, 5 * 365),
    ALL("전체 기간", null, null),
    ;

    fun sinceMs(): Long? = recentDays?.let { System.currentTimeMillis() - it * DAY_MS }
    fun untilMs(): Long? = olderThanDays?.let { System.currentTimeMillis() - it * DAY_MS }
}

enum class MediaType(val label: String) {
    ALL("전체"), PHOTO_ONLY("사진만"), VIDEO_ONLY("동영상만"),
}

enum class MinSize(val label: String, val bytes: Long) {
    NONE("제한 없음", 0L),
    KB500("500KB 이상", 500 * 1024L),
    MB1("1MB 이상", 1 * 1024 * 1024L),
    MB3("3MB 이상", 3 * 1024 * 1024L),
    MB5("5MB 이상", 5 * 1024 * 1024L),
}

enum class MaxVideoDuration(val label: String, val ms: Long) {
    ALL("전체", Long.MAX_VALUE),
    ONE_SEC("1초 이하", 1_000L),
    FIVE_SEC("5초 이하", 5_000L),
    THIRTY_SEC("30초 이하", 30_000L),
    ONE_MIN("1분 이하", 60_000L),
}
