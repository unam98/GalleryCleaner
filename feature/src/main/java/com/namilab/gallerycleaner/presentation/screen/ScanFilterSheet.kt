package com.namilab.gallerycleaner.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.namilab.gallerycleaner.feature.R
import com.namilab.gallerycleaner.domain.model.MaxVideoDuration
import com.namilab.gallerycleaner.domain.model.MediaType
import com.namilab.gallerycleaner.domain.model.MinSize
import com.namilab.gallerycleaner.domain.model.ScanAgeMarker
import com.namilab.gallerycleaner.domain.model.ScanFilter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanFilterSheet(
    filter: ScanFilter,
    onFilterChange: (ScanFilter) -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
    isPremium: Boolean = false,
    onPremiumRequired: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.scan_filter_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            // 미디어 종류
            Text(stringResource(R.string.media_type_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MediaType.entries.forEach { type ->
                    FilterChip(
                        selected = filter.mediaType == type,
                        onClick = { onFilterChange(filter.copy(mediaType = type)) },
                        label = { Text(type.label) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 기간 — 양쪽 핸들로 구간 선택 (예: 6개월~1년 사이). 오른쪽 끝 "전체"는 프리미엄 잠금
            PeriodRangeSlider(
                recentCutoffDays = filter.recentCutoffDays,
                oldCutoffDays = filter.oldCutoffDays,
                isPremium = isPremium,
                onRangeChange = { recent, old -> onFilterChange(filter.copy(recentCutoffDays = recent, oldCutoffDays = old)) },
                onLockedSelected = onPremiumRequired,
            )

            // 사진/전체일 때만 최소 크기 표시
            if (filter.mediaType != MediaType.VIDEO_ONLY) {
                Spacer(Modifier.height(20.dp))
                val sizes = MinSize.entries
                OrdinalSlider(
                    label = stringResource(R.string.min_size_section),
                    labelForIndex = { index -> sizes[index].label },
                    selectedIndex = sizes.indexOfFirst { it.bytes == filter.minSizeBytes }.coerceAtLeast(0),
                    lastIndex = sizes.lastIndex,
                    onIndexChange = { index -> onFilterChange(filter.copy(minSizeBytes = sizes[index].bytes)) },
                )
            }

            // 동영상/전체일 때만 영상 길이 표시
            if (filter.mediaType != MediaType.PHOTO_ONLY) {
                Spacer(Modifier.height(20.dp))
                val durations = MaxVideoDuration.entries
                OrdinalSlider(
                    label = "영상 길이",
                    labelForIndex = { index -> durations[index].label },
                    selectedIndex = durations.indexOfFirst { it.ms == filter.maxVideoDurationMs }.coerceAtLeast(0),
                    lastIndex = durations.lastIndex,
                    onIndexChange = { index -> onFilterChange(filter.copy(maxVideoDurationMs = durations[index].ms)) },
                )
            }

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = {
                    onDismiss()
                    onScan()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.start_scan))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * 기간을 "최근 N" 또는 "N 이상 된" 단일 컷오프가 아니라 실제 구간(예: 6개월~1년 사이)으로 고를 수 있는
 * 양쪽 핸들 슬라이더. 왼쪽 끝(지금)에 두면 "최근 N"과, 오른쪽 끝(전체)에 두면 "N 이상 된"과 동일해진다.
 */
@Composable
private fun PeriodRangeSlider(
    recentCutoffDays: Int?,
    oldCutoffDays: Int?,
    isPremium: Boolean,
    onRangeChange: (recentCutoffDays: Int?, oldCutoffDays: Int?) -> Unit,
    onLockedSelected: () -> Unit,
) {
    val markers = ScanAgeMarker.entries
    val lastIndex = markers.lastIndex

    val committedLeft = if (recentCutoffDays == null) 0
        else markers.indexOfFirst { it.days == recentCutoffDays }.let { if (it < 0) 0 else it }
    val committedRight = if (oldCutoffDays == null) lastIndex
        else markers.indexOfFirst { it.days == oldCutoffDays }.let { if (it < 0) lastIndex else it }

    var dragRange by remember(committedLeft, committedRight) {
        mutableStateOf(committedLeft.toFloat()..committedRight.toFloat())
    }
    LaunchedEffect(committedLeft, committedRight) {
        dragRange = committedLeft.toFloat()..committedRight.toFloat()
    }

    val previewLeft = dragRange.start.roundToInt().coerceIn(0, lastIndex)
    val previewRight = dragRange.endInclusive.roundToInt().coerceIn(0, lastIndex)
    val previewLocked = previewRight == lastIndex && !isPremium

    val leftLabel = markers[previewLeft].label
    val rightLabel = markers[previewRight].label
    val displayText = when {
        previewLeft == 0 && previewRight == lastIndex -> "전체 기간"
        previewLeft == 0 -> "최근 $rightLabel"
        previewRight == lastIndex -> "$leftLabel 이상 된"
        else -> "$leftLabel ~ $rightLabel 사이"
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.period_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    displayText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (previewLocked) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        RangeSlider(
            value = dragRange,
            onValueChange = { dragRange = it },
            onValueChangeFinished = {
                val left = dragRange.start.roundToInt().coerceIn(0, lastIndex)
                val right = dragRange.endInclusive.roundToInt().coerceIn(0, lastIndex)
                if (right == lastIndex && !isPremium) {
                    dragRange = committedLeft.toFloat()..committedRight.toFloat()
                    onLockedSelected()
                } else {
                    val newRecent = if (left == 0) null else markers[left].days
                    val newOld = if (right == lastIndex) null else markers[right].days
                    onRangeChange(newRecent, newOld)
                }
            },
            valueRange = 0f..lastIndex.toFloat(),
            steps = (lastIndex - 1).coerceAtLeast(0),
        )
    }
}

/**
 * 순서가 있는 옵션(크기·길이)을 항목 탭 대신 슬라이더로 고른다.
 * 드래그 중엔 로컬 상태로만 미리보기하고, 손을 뗄 때(onValueChangeFinished)만 실제 값을 커밋한다 —
 * 그래야 잠금 항목을 스치듯 지나가도 매 프레임 프리미엄 유도가 뜨지 않는다.
 */
@Composable
private fun OrdinalSlider(
    label: String,
    labelForIndex: (Int) -> String,
    selectedIndex: Int,
    lastIndex: Int,
    isLocked: (Int) -> Boolean = { false },
    onIndexChange: (Int) -> Unit,
    onLockedSelected: () -> Unit = {},
) {
    var dragIndex by remember(selectedIndex) { mutableFloatStateOf(selectedIndex.toFloat()) }
    LaunchedEffect(selectedIndex) { dragIndex = selectedIndex.toFloat() }

    val previewIndex = dragIndex.roundToInt().coerceIn(0, lastIndex)
    val previewLocked = isLocked(previewIndex)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    labelForIndex(previewIndex),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (previewLocked) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Slider(
            value = dragIndex,
            onValueChange = { dragIndex = it },
            onValueChangeFinished = {
                val idx = dragIndex.roundToInt().coerceIn(0, lastIndex)
                if (isLocked(idx)) {
                    dragIndex = selectedIndex.toFloat()
                    onLockedSelected()
                } else {
                    onIndexChange(idx)
                }
            },
            valueRange = 0f..lastIndex.toFloat(),
            steps = (lastIndex - 1).coerceAtLeast(0),
            colors = SliderDefaults.colors(),
        )
    }
}
