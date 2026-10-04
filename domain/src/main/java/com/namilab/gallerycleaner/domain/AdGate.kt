package com.namilab.gallerycleaner.domain

import android.app.Activity

interface AdGate {
    /** 리워드 광고 시청 후 1시간 타이머가 아직 유효하면 true */
    fun isUnlocked(): Boolean
    /** 리워드 광고 시청 완료 시 1시간 타이머 시작 */
    fun unlock()
    /** 남은 시간(ms), 0이면 만료 */
    fun remainingMs(): Long
    /** 유료 구매 완료 여부 — true면 광고 없이 모든 기능 사용 가능 */
    fun isPremium(): Boolean
    /** 유료 구매 상태 로컬 캐시 갱신 (Billing 콜백·디버그 토글에서 사용) */
    fun setPremium(value: Boolean)
    /** Google Play Billing 구매 플로우 시작. 구매 완료·복원 시 onPremiumChanged가 true로 호출됨 */
    fun launchPremiumPurchase(activity: Activity)
    /** 프리미엄 상태 변경 구독 (Billing 콜백, 복원 구매 등 비동기 변경 반영용) */
    fun addOnPremiumChangedListener(listener: (Boolean) -> Unit)
}
