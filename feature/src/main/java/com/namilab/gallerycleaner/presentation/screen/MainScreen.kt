package com.namilab.gallerycleaner.presentation.screen

import android.Manifest
import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.namilab.gallerycleaner.feature.R
import com.namilab.gallerycleaner.presentation.MainEvent
import com.namilab.gallerycleaner.presentation.MainViewModel
import com.namilab.gallerycleaner.presentation.UiState

private enum class AppTab { SCAN, TOURNAMENT, SETTINGS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onShowRewardedAd: (onRewarded: () -> Unit, onFailed: () -> Unit, onLoadingEnded: () -> Unit) -> Unit =
        { onRewarded, _, onLoadingEnded -> onRewarded(); onLoadingEnded() },
    scanningBanner: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val filteredGroups by viewModel.filteredGroups.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val availableCategories by viewModel.availableCategories.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val periodicNotification by viewModel.periodicNotification.collectAsStateWithLifecycle()
    val screenshotNotification by viewModel.screenshotNotification.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val tournamentState by viewModel.tournamentState.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(AppTab.SCAN) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showAdDialog by remember { mutableStateOf(false) }
    var isPreparingAd by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

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
                is MainEvent.ShowRewardedAdDialog -> showAdDialog = true
                is MainEvent.ShowPremiumPrompt -> showPremiumDialog = true
            }
        }
    }

    // 최초 실행 온보딩 — 미디어 권한 요청 전 가치 설명
    if (!onboardingCompleted) {
        OnboardingScreen(onComplete = { viewModel.completeOnboarding() })
        return
    }

    // 이상형 월드컵 오버레이 (그룹 상세보다 위)
    tournamentState?.let { ts ->
        TournamentScreen(
            state = ts,
            onPick = { viewModel.pickInTournament(it) },
            onClose = { viewModel.closeTournament() },
            onSkipGroup = { viewModel.skipTournamentGroup() },
            onKeepWinner = { viewModel.keepTournamentWinner(it) },
        )
        return
    }

    // 그룹 상세 화면은 탭 위에 오버레이
    if (selectedGroup != null) {
        GroupDetailScreen(
            group = selectedGroup!!,
            favoriteIds = favoriteIds,
            onBack = { viewModel.clearGroupSelection() },
            onDelete = { ids -> viewModel.requestDelete(ids) },
            onToggleFavorite = { viewModel.toggleFavorite(it) },
            onStartTournament = { viewModel.startTournament(selectedGroup!!) },
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
                        AppTab.TOURNAMENT -> null
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
                    selectedCategories = selectedCategories,
                    availableCategories = availableCategories,
                    sortOrder = sortOrder,
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
                    onCategoryToggle = { viewModel.toggleCategory(it) },
                    onClearCategories = { viewModel.clearCategories() },
                    onSortOrderChange = { viewModel.setSortOrder(it) },
                    onGroupClick = { viewModel.selectGroup(it) },
                    onQuickDelete = { group ->
                        val toDelete = group.photos.filter { it.id != group.bestPhotoId }
                        if (toDelete.isNotEmpty()) viewModel.requestDelete(toDelete)
                    },
                    onRetry = { viewModel.reset() },
                    scanningBanner = scanningBanner,
                )

                AppTab.TOURNAMENT -> TournamentTabContent(
                    state = state,
                    onStart = { viewModel.startTournamentQueue() },
                )

                AppTab.SETTINGS -> SettingsContent(
                    periodicNotification = periodicNotification,
                    screenshotNotification = screenshotNotification,
                    onPeriodicNotificationChange = { viewModel.setPeriodicNotification(it) },
                    onScreenshotNotificationChange = { viewModel.setScreenshotNotification(it) },
                    isPremium = isPremium,
                    onPurchasePremium = { showPremiumDialog = true },
                    onDebugTriggerPeriodicScan = { viewModel.debugTriggerPeriodicScan() },
                    onDebugTriggerScreenshotNotif = { viewModel.debugTriggerScreenshotNotif() },
                    onDebugTriggerScanDoneNotif = { viewModel.debugTriggerScanDoneNotif() },
                    onDebugTogglePremium = { viewModel.debugTogglePremium() },
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
            isPremium = isPremium,
            onPremiumRequired = { viewModel.requestPremiumPrompt() },
        )
    }

    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            onConfirm = {
                showPremiumDialog = false
                (context as? Activity)?.let { viewModel.purchasePremium(it) }
            },
            onDismiss = { showPremiumDialog = false },
        )
    }

    if (showAdDialog) {
        RewardedAdDialog(
            onConfirm = {
                showAdDialog = false
                showFilterSheet = false
                isPreparingAd = true
                onShowRewardedAd(
                    { viewModel.onAdRewarded() },
                    {
                        Toast.makeText(context, context.getString(R.string.ad_load_failed), Toast.LENGTH_SHORT).show()
                    },
                    { isPreparingAd = false },
                )
            },
            onDismiss = { showAdDialog = false },
        )
    }

    if (isPreparingAd) {
        AdLoadingOverlay()
    }
}

@Composable
private fun RewardedAdDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("1시간 광고 없이 스캔하기") },
        text = { Text("동영상 광고 하나 보면\n1시간 동안 자유롭게 스캔하고 정리할 수 있어요.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("광고 보고 시작하기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}

/** 리워드 광고 로딩 중 화면을 막고 진행 상태를 보여준다 — 안 막으면 뒤 화면이 계속 터치돼서 멈춘 것처럼 보인다. */
@Composable
private fun AdLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.ad_loading),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PremiumUpgradeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("프리미엄 전용 기능") },
        text = { Text("전체 기간 스캔은 프리미엄 구매 후 이용할 수 있어요.\n광고 없이 모든 기능을 무제한으로 사용할 수 있습니다.") },
        confirmButton = {
            Button(onClick = onConfirm) { Text("구매하기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        },
    )
}

@Composable
private fun AppNavigationBar(selectedTab: AppTab, onTabSelect: (AppTab) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        listOf(
            Triple(AppTab.SCAN, Icons.Rounded.PhotoLibrary, R.string.tab_scan),
            Triple(AppTab.TOURNAMENT, Icons.Rounded.EmojiEvents, R.string.tab_tournament),
            Triple(AppTab.SETTINGS, Icons.Rounded.Settings, R.string.settings),
        ).forEach { (tab, icon, labelRes) ->
            val selected = selectedTab == tab
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelect(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
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
    filteredGroups: List<com.namilab.gallerycleaner.domain.model.PhotoGroup>,
    selectedCategories: Set<String>,
    availableCategories: Set<String>,
    sortOrder: com.namilab.gallerycleaner.presentation.GroupSortOrder,
    favoriteIds: Set<Long>,
    permissionGranted: Boolean,
    onScan: () -> Unit,
    onCategoryToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    onSortOrderChange: (com.namilab.gallerycleaner.presentation.GroupSortOrder) -> Unit,
    onGroupClick: (com.namilab.gallerycleaner.domain.model.PhotoGroup) -> Unit,
    onQuickDelete: (com.namilab.gallerycleaner.domain.model.PhotoGroup) -> Unit,
    onRetry: () -> Unit,
    scanningBanner: @Composable () -> Unit = {},
) {
    when (val s = state) {
        is UiState.Idle -> IdleContent(
            permissionGranted = permissionGranted,
            onScan = onScan,
        )
        is UiState.Scanning -> ScanningIndicator(s, banner = scanningBanner)
        is UiState.Done -> GroupListScreen(
            groups = filteredGroups,
            totalGroupCount = s.groups.size,
            selectedCategories = selectedCategories,
            availableCategories = availableCategories,
            sortOrder = sortOrder,
            favoriteIds = favoriteIds,
            onCategoryToggle = onCategoryToggle,
            onClearCategories = onClearCategories,
            onSortOrderChange = onSortOrderChange,
            onGroupClick = onGroupClick,
            onQuickDelete = onQuickDelete,
        )
        is UiState.Error -> ErrorContent(onRetry = onRetry)
    }
}

@Composable
private fun TournamentTabContent(state: UiState, onStart: () -> Unit) {
    val eligibleCount = (state as? UiState.Done)?.groups
        ?.count { it.photos.size >= 2 && it.photos.none { p -> p.isVideo } } ?: 0

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
                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    RoundedCornerShape(24.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.EmojiEvents,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            stringResource(R.string.tournament_tab_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.tournament_tab_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.weight(1f))

        when {
            state !is UiState.Done -> {
                Text(
                    stringResource(R.string.tournament_tab_empty_no_scan),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            eligibleCount == 0 -> {
                Text(
                    stringResource(R.string.tournament_tab_empty_no_groups),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                Text(
                    stringResource(R.string.tournament_groups_waiting, eligibleCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                    ),
                ) {
                    Text(
                        stringResource(R.string.tournament_tab_cta),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun IdleContent(
    permissionGranted: Boolean,
    onScan: () -> Unit,
) {
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
                Icons.Rounded.PhotoLibrary,
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
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            stringResource(R.string.idle_subtitle_detail),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(12.dp),
            )
            Text(
                stringResource(R.string.home_privacy_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.weight(1f))

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(216.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
            )
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            )
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onScan,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (permissionGranted) stringResource(R.string.scan_button_label)
                        else stringResource(R.string.grant_access_button_label),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        StorageUsageCard()

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StorageUsageCard() {
    val (usedBytes, totalBytes) = remember {
        val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        (total - free) to total
    }
    val usageRatio = if (totalBytes > 0) usedBytes.toFloat() / totalBytes else 0f
    val animProgress by animateFloatAsState(
        targetValue = usageRatio,
        animationSpec = tween(durationMillis = 900),
        label = "storageProgress",
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.storage_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (totalBytes > 0)
                        "%.1f / %.0f GB".format(usedBytes / 1_073_741_824.0, totalBytes / 1_073_741_824.0)
                    else stringResource(R.string.storage_measuring),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
            Spacer(Modifier.height(10.dp))
            val statusRes = when {
                usageRatio >= 0.9f -> R.string.storage_status_critical
                usageRatio >= 0.7f -> R.string.storage_status_warning
                else -> R.string.storage_status_ok
            }
            val statusColor = when {
                usageRatio >= 0.9f -> MaterialTheme.colorScheme.error
                usageRatio >= 0.7f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Text(
                stringResource(statusRes),
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun ScanningIndicator(s: UiState.Scanning, banner: @Composable () -> Unit = {}) {
    val etaText = if (s.etaMs > 0 && s.current > 0 && s.current < s.total) {
        when {
            s.etaMs < 60_000 -> {
                val secs = (s.etaMs / 1000).coerceAtLeast(1)
                stringResource(R.string.scanning_eta_seconds, secs)
            }
            else -> {
                val minutes = ((s.etaMs + 30_000) / 60_000).coerceAtLeast(1)
                stringResource(R.string.scanning_eta_minutes, minutes)
            }
        }
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        ScanningTips(modifier = Modifier.weight(1f))
        Spacer(Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    when {
                        s.isLabelingPhase -> stringResource(R.string.labeling_phase)
                        s.total == 0 -> stringResource(R.string.loading_photos)
                        s.label.isNotEmpty() -> s.label
                        else -> stringResource(R.string.scanning_phase_finding)
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(14.dp))
                if (s.total > 0) {
                    val progress = s.current.toFloat() / s.total
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (s.isLabelingPhase) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (s.isLabelingPhase) stringResource(R.string.labeling_desc)
                            else etaText ?: stringResource(R.string.scanning_progress, s.current, s.total),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "%.0f%%".format(progress * 100),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (s.isLabelingPhase) MaterialTheme.colorScheme.tertiary
                                    else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.scanning_hint_background),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        banner()
        Spacer(Modifier.height(8.dp))
    }
}

private data class ScanTip(val emoji: String, val title: String, val body: String)

private val SCAN_TIPS = listOf(
    ScanTip("💡", "알고 계셨나요?", "중복 사진을 주기적으로 정리하면\n폰이 더 빠르게 반응해요."),
    ScanTip("📸", "버스트 사진 정리", "연속 촬영한 사진은 보통 한 장만 남겨도\n소중한 순간은 충분히 담겨요."),
    ScanTip("💾", "용량 걱정 끝", "정기 정리를 하면\n용량 부족 걱정 없이 언제든 사진을 찍을 수 있어요."),
    ScanTip("⭐", "더 철저하게 정리하려면", "프리미엄으로 전체 기간을 스캔하면\n더 많은 용량을 확보할 수 있어요."),
)

@Composable
private fun ScanningTips(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SCAN_TIPS.forEach { tip -> ScanTipCard(tip) }
    }
}

@Composable
private fun ScanTipCard(tip: ScanTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(tip.emoji, style = MaterialTheme.typography.titleMedium)
            }
            Column {
                Text(
                    tip.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tip.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.error_message),
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
