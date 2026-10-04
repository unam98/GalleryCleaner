package com.namilab.gallerycleaner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ScanFilter(
    /** 왼쪽(최신 쪽) 핸들 — 사진이 최소 이만큼은 오래돼야 함. null이면 제한 없음(가장 최근 사진부터 포함) */
    val recentCutoffDays: Int? = null,
    /** 오른쪽(오래된 쪽) 핸들 — 사진이 최대 이만큼까지만 오래될 수 있음. null이면 제한 없음(가장 오래된 사진까지 포함) */
    val oldCutoffDays: Int? = null,
    val minSizeBytes: Long = 0L,
    val maxVideoDurationMs: Long = Long.MAX_VALUE,
    val customSinceMs: Long? = null,
    val mediaType: MediaType = MediaType.ALL,
) : Parcelable {
    fun sinceTimestampMs(): Long? = customSinceMs ?: oldCutoffDays?.let { System.currentTimeMillis() - it * DAY_MS }
    fun untilTimestampMs(): Long? = recentCutoffDays?.let { System.currentTimeMillis() - it * DAY_MS }
}

private val DAY_MS = 86_400_000L

/**
 * "기간" 범위 슬라이더의 양쪽 핸들이 짚을 수 있는 눈금.
 * NOW(0)와 ALL(무제한)을 양 끝에 두고, 두 핸들이 이 리스트 위 서로 다른 지점을 가리키면
 * "6개월~1년 사이" 같은 구간 선택이 된다. 한쪽 핸들만 극단에 두면 기존 "최근 N" / "N 이상 된"과 동일해진다.
 */
enum class ScanAgeMarker(val label: String, val days: Int?) {
    NOW("지금", 0),
    WEEK("1주", 7),
    MONTH("1개월", 30),
    THREE_MONTHS("3개월", 90),
    SIX_MONTHS("6개월", 180),
    ONE_YEAR("1년", 365),
    TWO_YEARS("2년", 730),
    ALL("전체", null),
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

// 슬라이더 값이 왼쪽(짧음)→오른쪽(김)으로 자연스럽게 커지도록, 무제한(전체)을 맨 끝에 둔다.
// ALL이 맨 앞에 있으면 슬라이더를 오른쪽 끝까지 밀어도 "1분 이하"에서 막혀 긴 영상이 계속 제외된다.
enum class MaxVideoDuration(val label: String, val ms: Long) {
    ONE_SEC("1초 이하", 1_000L),
    FIVE_SEC("5초 이하", 5_000L),
    THIRTY_SEC("30초 이하", 30_000L),
    ONE_MIN("1분 이하", 60_000L),
    ALL("전체", Long.MAX_VALUE),
}
