package com.unam.photocleaner.presentation.screen

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.unam.photocleaner.R
import com.unam.photocleaner.presentation.MainEvent
import com.unam.photocleaner.presentation.ui.theme.iOSGreen
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
    val videoPermissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permission = rememberPermissionState(permissionName)
    val videoPermission = rememberPermissionState(videoPermissionName)

    // POST_NOTIFICATIONS 권한 요청 (Android 13+, 미허용 시 스크린샷 알림 무음 실패)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(notifPermission.status.isGranted) {
            if (!notifPermission.status.isGranted) {
                notifPermission.launchPermissionRequest()
            }
        }
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteConfirmed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.RequestSystemDelete -> {
                    deleteLauncher.launch(IntentSenderRequest.Builder(event.intentSender).build())
                }
            }
        }
    }

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    if (state is UiState.Done || state is UiState.Scanning) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                },
                actions = {
                    if (state is UiState.Done) {
                        TextButton(onClick = { showFilterSheet = true }) {
                            Text(
                                stringResource(R.string.rescan),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                is UiState.Idle -> IdleContent(
                    permissionGranted = permission.status.isGranted,
                    onScan = {
                        if (permission.status.isGranted) {
                            if (!videoPermission.status.isGranted) videoPermission.launchPermissionRequest()
                            showFilterSheet = true
                        } else {
                            permission.launchPermissionRequest()
                        }
                    },
                )

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
                    onQuickDelete = { group ->
                        val toDelete = group.photos.filter { it.id != group.bestPhotoId }
                        if (toDelete.isNotEmpty()) viewModel.requestDelete(toDelete)
                    },
                )

                is UiState.Error -> ErrorContent(message = s.message, onRetry = { viewModel.reset() })
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
private fun IdleContent(permissionGranted: Boolean, onScan: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        // 앱 아이콘
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "중복·유사 사진과 영상을 찾아\n소중한 저장 공간을 확보하세요",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        // 기능 목록 카드
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column {
                FeatureRow(
                    icon = Icons.Outlined.AutoDelete,
                    iconBg = MaterialTheme.colorScheme.primary,
                    label = "중복·유사 사진 자동 탐지",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                FeatureRow(
                    icon = Icons.Outlined.Videocam,
                    iconBg = iOSGreen,
                    label = "중복 영상·짧은 클립 정리",
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                FeatureRow(
                    icon = Icons.Outlined.Star,
                    iconBg = MaterialTheme.colorScheme.tertiary,
                    label = "즐겨찾기로 실수 삭제 방지",
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = onScan,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Text(
                if (permissionGranted) stringResource(R.string.start_scan)
                else stringResource(R.string.grant_media_access),
                style = MaterialTheme.typography.titleSmall,
            )
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, iconBg: Color, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(iconBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ScanningIndicator(s: UiState.Scanning) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    if (s.total == 0) stringResource(R.string.loading_photos)
                    else if (s.label.isNotEmpty()) s.label else "분석 중…",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (s.total > 0) {
                    val progress = s.current.toFloat() / s.total
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.scanning_progress, s.current, s.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "%.0f%%".format(progress * 100),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.error_message, message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.retry))
        }
    }
}
