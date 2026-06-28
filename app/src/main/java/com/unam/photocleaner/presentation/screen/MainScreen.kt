package com.unam.photocleaner.presentation.screen

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.unam.photocleaner.presentation.MainEvent
import com.unam.photocleaner.presentation.MainViewModel
import com.unam.photocleaner.presentation.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val filteredGroups by viewModel.filteredGroups.collectAsStateWithLifecycle()
    val isLabeling by viewModel.isLabeling.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val keyword by viewModel.keyword.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val periodicNotification by viewModel.periodicNotification.collectAsStateWithLifecycle()
    val screenshotNotification by viewModel.screenshotNotification.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permission = rememberPermissionState(permissionName)

    // Android 10+ 시스템 삭제 다이얼로그 런처
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteConfirmed()
        }
    }

    // 시스템 삭제 다이얼로그 이벤트 수신
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.RequestSystemDelete -> {
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
            }
        }
    }

    // 그룹 상세 화면
    if (selectedGroup != null) {
        GroupDetailScreen(
            group = selectedGroup!!,
            favoriteIds = favoriteIds,
            onBack = { viewModel.clearGroupSelection() },
            onDelete = { ids -> viewModel.requestDelete(ids) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PhotoCleaner") },
                actions = {
                    if (state is UiState.Done) {
                        TextButton(onClick = { showFilterSheet = true }) {
                            Text("재스캔")
                        }
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is UiState.Idle -> {
                    Button(onClick = {
                        if (permission.status.isGranted) showFilterSheet = true
                        else permission.launchPermissionRequest()
                    }) {
                        Text(if (permission.status.isGranted) "스캔 시작" else "사진 접근 허용")
                    }
                }

                is UiState.Scanning -> ScanningIndicator(s)

                is UiState.Done -> GroupListScreen(
                    groups = filteredGroups,
                    totalSaving = s.totalSavingBytes,
                    totalGroupCount = s.groups.size,
                    isLabeling = isLabeling,
                    selectedCategory = selectedCategory,
                    keyword = keyword,
                    onCategorySelect = { viewModel.setCategory(it) },
                    onKeywordChange = { viewModel.setKeyword(it) },
                    onGroupClick = { group -> viewModel.selectGroup(group) },
                )

                is UiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("오류: ${s.message}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.reset() }) { Text("다시 시도") }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        ScanFilterSheet(
            filter = filter,
            onFilterChange = { viewModel.updateFilter(it) },
            onScan = { viewModel.scan() },
            onDismiss = { showFilterSheet = false },
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            periodicNotification = periodicNotification,
            screenshotNotification = screenshotNotification,
            onPeriodicNotificationChange = { viewModel.setPeriodicNotification(it) },
            onScreenshotNotificationChange = { viewModel.setScreenshotNotification(it) },
            onDismiss = { showSettingsSheet = false },
        )
    }
}

@Composable
private fun ScanningIndicator(s: UiState.Scanning) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(260.dp),
    ) {
        if (s.total == 0) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text("사진 목록 불러오는 중...", style = MaterialTheme.typography.bodyMedium)
        } else {
            val progress = s.current.toFloat() / s.total
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "%,d / %,d장".format(s.current, s.total),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (s.label.isNotEmpty()) s.label else "%.0f%%".format(progress * 100),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
