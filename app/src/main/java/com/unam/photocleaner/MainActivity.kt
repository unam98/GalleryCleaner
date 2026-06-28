package com.unam.photocleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.unam.photocleaner.presentation.MainViewModel
import com.unam.photocleaner.presentation.screen.MainScreen
import com.unam.photocleaner.presentation.ui.theme.PhotoCleanerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 알림 탭으로 진입 시 바로 스캔 시작
        if (intent.getBooleanExtra(EXTRA_AUTO_SCAN, false)) {
            viewModel.scan()
        }

        setContent {
            PhotoCleanerTheme {
                MainScreen(viewModel)
            }
        }
    }

    companion object {
        const val EXTRA_AUTO_SCAN = "extra_auto_scan"
    }
}
