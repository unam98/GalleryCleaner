package com.namilab.gallerycleaner.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.namilab.gallerycleaner.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    fun preload() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                }
            },
        )
    }

    /**
     * 광고가 이미 로드돼 있으면 바로 보여준다. 아직 준비 안 됐으면 그 자리에서 로드부터 하고,
     * 로드가 끝나야 보여준다 — 유저가 실제로 광고를 끝까지 보기 전에는 [onRewarded]를 호출하지 않는다.
     * [onLoadingEnded]는 "로딩 중" 표시를 꺼도 되는 시점(광고 화면이 실제로 떴을 때, 또는 로드/표시 자체가
     * 실패했을 때)에 호출된다 — 로드하는 동안 화면이 계속 터치되는데 아무 반응이 없어 보이는 걸 막기 위한 용도.
     */
    fun show(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit = {}, onLoadingEnded: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad != null) {
            showLoadedAd(activity, ad, onRewarded, onLoadingEnded)
            return
        }
        isLoading = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoading = false
                    showLoadedAd(activity, ad, onRewarded, onLoadingEnded)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading = false
                    rewardedAd = null
                    onFailed()
                    onLoadingEnded()
                }
            },
        )
    }

    private fun showLoadedAd(activity: Activity, ad: RewardedAd, onRewarded: () -> Unit, onLoadingEnded: () -> Unit) {
        rewardedAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() { onLoadingEnded() }
            override fun onAdDismissedFullScreenContent() { preload() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { preload(); onLoadingEnded() }
        }
        ad.show(activity) { onRewarded() }
    }
}
