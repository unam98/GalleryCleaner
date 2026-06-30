package com.namilab.gallerycleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.namilab.gallerycleaner.ads.BannerAdView
import com.namilab.gallerycleaner.ads.RewardedAdManager
import com.namilab.gallerycleaner.presentation.MainViewModel
import com.namilab.gallerycleaner.presentation.screen.MainScreen
import com.namilab.gallerycleaner.presentation.ui.theme.GalleryCleanerTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    @Inject lateinit var rewardedAdManager: RewardedAdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        rewardedAdManager.preload()

        if (intent.getBooleanExtra(EXTRA_NAVIGATE_TOP_GROUP, false)) {
            viewModel.navigateToTopGroup()
        } else if (intent.getBooleanExtra(EXTRA_AUTO_SCAN, false)) {
            val sinceMs = intent.getLongExtra(EXTRA_SCAN_SINCE_MS, -1L).takeIf { it >= 0 }
            viewModel.scan(overrideSinceMs = sinceMs)
        }

        setContent {
            GalleryCleanerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onShowRewardedAd = { onRewarded ->
                        rewardedAdManager.show(this, onRewarded = onRewarded)
                    },
                    scanningBanner = { BannerAdView() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_SCAN = "extra_auto_scan"
        const val EXTRA_SCAN_SINCE_MS = "extra_scan_since_ms"
        const val EXTRA_NAVIGATE_TOP_GROUP = "extra_navigate_top_group"
    }
}
