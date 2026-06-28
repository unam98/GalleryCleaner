package com.unam.photocleaner.presentation.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unam.photocleaner.R
import com.unam.photocleaner.domain.model.MaxPhotoCount
import com.unam.photocleaner.domain.model.MediaType
import com.unam.photocleaner.domain.model.MinSize
import com.unam.photocleaner.domain.model.ScanFilter
import com.unam.photocleaner.domain.model.ScanPeriod

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanFilterSheet(
    filter: ScanFilter,
    onFilterChange: (ScanFilter) -> Unit,
    onScan: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text(stringResource(R.string.scan_filter_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

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

            Text(stringResource(R.string.period_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanPeriod.entries.forEach { period ->
                    FilterChip(
                        selected = filter.period == period,
                        onClick = { onFilterChange(filter.copy(period = period)) },
                        label = { Text(period.label) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(stringResource(R.string.photo_count_section), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MaxPhotoCount.entries.forEach { option ->
                    FilterChip(
                        selected = filter.maxPhotoCount == option.count,
                        onClick = { onFilterChange(filter.copy(maxPhotoCount = option.count)) },
                        label = { Text(option.label) },
                    )
                }
            }

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
