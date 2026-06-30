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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.namilab.gallerycleaner.feature.R
import com.namilab.gallerycleaner.domain.model.MaxVideoDuration
import com.namilab.gallerycleaner.domain.model.MediaType
import com.namilab.gallerycleaner.domain.model.MinSize
import com.namilab.gallerycleaner.domain.model.ScanFilter
import com.namilab.gallerycleaner.domain.model.ScanPeriod

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

            // 기간
            Text(stringResource(R.string.period_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanPeriod.entries.forEach { period ->
                    val locked = period == ScanPeriod.ALL && !isPremium
                    FilterChip(
                        selected = filter.period == period,
                        onClick = {
                            if (locked) onPremiumRequired()
                            else onFilterChange(filter.copy(period = period))
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(period.label)
                                if (locked) {
                                    Icon(
                                        Icons.Outlined.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        },
                        colors = if (locked) FilterChipDefaults.filterChipColors(
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        ) else FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            // 사진/전체일 때만 최소 크기 표시
            if (filter.mediaType != MediaType.VIDEO_ONLY) {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.min_size_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MinSize.entries.forEach { size ->
                        FilterChip(
                            selected = filter.minSizeBytes == size.bytes,
                            onClick = { onFilterChange(filter.copy(minSizeBytes = size.bytes)) },
                            label = { Text(size.label) },
                        )
                    }
                }
            }

            // 동영상/전체일 때만 영상 길이 표시
            if (filter.mediaType != MediaType.PHOTO_ONLY) {
                Spacer(Modifier.height(20.dp))
                Text("영상 길이", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MaxVideoDuration.entries.forEach { dur ->
                        FilterChip(
                            selected = filter.maxVideoDurationMs == dur.ms,
                            onClick = { onFilterChange(filter.copy(maxVideoDurationMs = dur.ms)) },
                            label = { Text(dur.label) },
                        )
                    }
                }
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
