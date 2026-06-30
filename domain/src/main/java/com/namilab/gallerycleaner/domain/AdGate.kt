package com.namilab.gallerycleaner.domain

interface AdGate {
    /** 리워드 광고 시청 후 1시간 타이머가 아직 유효하면 true */
    fun isUnlocked(): Boolean
    /** 리워드 광고 시청 완료 시 1시간 타이머 시작 */
    fun unlock()
    /** 남은 시간(ms), 0이면 만료 */
    fun remainingMs(): Long
    /** 유료 구매 완료 여부 — true면 광고 없이 모든 기능 사용 가능 */
    fun isPremium(): Boolean
    /** 유료 구매 처리 (Play Billing 연동 전 임시) */
    fun setPremium(value: Boolean)
}
