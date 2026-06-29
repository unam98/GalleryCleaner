package com.unam.gallerycleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.unam.gallerycleaner.presentation.MainViewModel
import com.unam.gallerycleaner.presentation.screen.MainScreen
import com.unam.gallerycleaner.presentation.ui.theme.GalleryCleanerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 알림 탭으로 진입 시 워커가 스캔한 동일 기간으로 재스캔
        if (intent.getBooleanExtra(EXTRA_AUTO_SCAN, false)) {
            val sinceMs = intent.getLongExtra(EXTRA_SCAN_SINCE_MS, -1L).takeIf { it >= 0 }
            viewModel.scan(overrideSinceMs = sinceMs)
        }

        setContent {
            GalleryCleanerTheme {
                MainScreen(viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_SCAN = "extra_auto_scan"
        const val EXTRA_SCAN_SINCE_MS = "extra_scan_since_ms"
    }
}
