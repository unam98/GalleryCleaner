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

    fun preload() {
        if (rewardedAd != null) return
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
                override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
            },
        )
    }

    fun show(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit = {}) {
        val ad = rewardedAd
        if (ad == null) {
            // 광고 로드 실패 시 fallback: 그냥 보상 지급 (UX 우선)
            onRewarded()
            preload()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() { rewardedAd = null; preload() }
            override fun onAdFailedToShowFullScreenContent(e: AdError) { rewardedAd = null; onFailed() }
        }
        ad.show(activity) { onRewarded() }
        rewardedAd = null
    }
}
