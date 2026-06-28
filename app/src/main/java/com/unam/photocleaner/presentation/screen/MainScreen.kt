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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.unam.photocleaner.presentation.MainViewModel
import com.unam.photocleaner.presentation.UiState
import com.unam.photocleaner.presentation.ui.theme.iOSGreen

private enum class AppTab { SCAN, FAVORITES, SETTINGS }

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

    var selectedTab by rememberSaveable { mutableStateOf(AppTab.SCAN) }
    var showFilterSheet by remember { mutableStateOf(false) }

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

    // 그룹 상세 화면은 탭 위에 오버레이
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
                    val title = when (selectedTab) {
                        AppTab.SCAN -> if (state is UiState.Done || state is UiState.Scanning)
                            stringResource(R.string.app_name) else null
                        AppTab.FAVORITES -> stringResource(R.string.tab_favorites)
                        AppTab.SETTINGS -> stringResource(R.string.settings)
                    }
                    if (title != null) {
                        Text(title, style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    if (selectedTab == AppTab.SCAN && state is UiState.Done) {
                        TextButton(onClick = { showFilterSheet = true }) {
                            Text(
                                stringResource(R.string.rescan),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        bottomBar = {
            AppNavigationBar(selectedTab = selectedTab, onTabSelect = { selectedTab = it })
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                AppTab.SCAN -> ScanTabContent(
                    state = state,
                    filteredGroups = filteredGroups,
                    isLabeling = isLabeling,
                    selectedCategory = selectedCategory,
                    keyword = keyword,
                    favoriteIds = favoriteIds,
                    permissionGranted = permission.status.isGranted,
                    onScan = {
                        if (permission.status.isGranted) {
                            if (!videoPermission.status.isGranted) videoPermission.launchPermissionRequest()
                            showFilterSheet = true
                        } else {
                            permission.launchPermissionRequest()
                        }
                    },
                    onCategorySelect = { viewModel.setCategory(it) },
                    onKeywordChange = { viewModel.setKeyword(it) },
                    onGroupClick = { viewModel.selectGroup(it) },
                    onQuickDelete = { group ->
                        val toDelete = group.photos.filter { it.id != group.bestPhotoId }
                        if (toDelete.isNotEmpty()) viewModel.requestDelete(toDelete)
                    },
                    onRetry = { viewModel.reset() },
                )

                AppTab.FAVORITES -> FavoritesScreen(
                    scanState = state,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                )

                AppTab.SETTINGS -> SettingsContent(
                    periodicNotification = periodicNotification,
                    screenshotNotification = screenshotNotification,
                    onPeriodicNotificationChange = { viewModel.setPeriodicNotification(it) },
                    onScreenshotNotificationChange = { viewModel.setScreenshotNotification(it) },
                    modifier = Modifier.padding(top = 8.dp),
                )
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
}

@Composable
private fun AppNavigationBar(selectedTab: AppTab, onTabSelect: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        listOf(
            Triple(AppTab.SCAN, Icons.Outlined.PhotoLibrary, R.string.tab_scan),
            Triple(AppTab.FAVORITES, Icons.Outlined.Star, R.string.tab_favorites),
            Triple(AppTab.SETTINGS, Icons.Outlined.Settings, R.string.settings),
        ).forEach { (tab, icon, labelRes) ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected && tab == AppTab.FAVORITES) Icons.Filled.Star else icon,
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
private fun ScanTabContent(
    state: UiState,
    filteredGroups: List<com.unam.photocleaner.domain.model.PhotoGroup>,
    isLabeling: Boolean,
    selectedCategory: String?,
    keyword: String,
    favoriteIds: Set<Long>,
    permissionGranted: Boolean,
    onScan: () -> Unit,
    onCategorySelect: (String?) -> Unit,
    onKeywordChange: (String) -> Unit,
    onGroupClick: (com.unam.photocleaner.domain.model.PhotoGroup) -> Unit,
    onQuickDelete: (com.unam.photocleaner.domain.model.PhotoGroup) -> Unit,
    onRetry: () -> Unit,
) {
    when (val s = state) {
        is UiState.Idle -> IdleContent(permissionGranted = permissionGranted, onScan = onScan)
        is UiState.Scanning -> ScanningIndicator(s)
        is UiState.Done -> GroupListScreen(
            groups = filteredGroups,
            totalSaving = s.totalSavingBytes,
            totalGroupCount = s.groups.size,
            isLabeling = isLabeling,
            selectedCategory = selectedCategory,
            keyword = keyword,
            onCategorySelect = onCategorySelect,
            onKeywordChange = onKeywordChange,
            onGroupClick = onGroupClick,
            onQuickDelete = onQuickDelete,
        )
        is UiState.Error -> ErrorContent(message = s.message, onRetry = onRetry)
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
            stringResource(R.string.idle_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

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
                    label = stringResource(R.string.feature_auto_detect),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                FeatureRow(
                    icon = Icons.Outlined.Videocam,
                    iconBg = iOSGreen,
                    label = stringResource(R.string.feature_video_clean),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(start = 58.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                FeatureRow(
                    icon = Icons.Outlined.Star,
                    iconBg = MaterialTheme.colorScheme.tertiary,
                    label = stringResource(R.string.feature_favorite_protect),
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
                    else if (s.label.isNotEmpty()) s.label else stringResource(R.string.analyzing),
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
