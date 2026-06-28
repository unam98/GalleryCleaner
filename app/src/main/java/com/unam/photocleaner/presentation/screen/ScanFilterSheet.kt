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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
            Text("스캔 범위 설정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))

            Text("기간", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

            Text("최소 파일 크기", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text("스캔 시작")
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
