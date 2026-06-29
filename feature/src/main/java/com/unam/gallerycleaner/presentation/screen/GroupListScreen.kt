package com.unam.gallerycleaner.presentation.screen

import com.unam.gallerycleaner.domain.util.formatBytes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unam.gallerycleaner.feature.R
import com.unam.gallerycleaner.domain.model.GroupType
import com.unam.gallerycleaner.domain.model.Photo
import com.unam.gallerycleaner.domain.model.PhotoGroup
import com.unam.gallerycleaner.presentation.MainViewModel

@Composable
fun GroupListScreen(
    groups: List<PhotoGroup>,
    totalSaving: Long,
    totalGroupCount: Int,
    selectedCategories: Set<String>,
    availableCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    onGroupClick: (PhotoGroup) -> Unit,
    onQuickDelete: (PhotoGroup) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { SavingHeaderCard(totalSaving = totalSaving, totalGroupCount = totalGroupCount, filteredCount = groups.size) }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            CategoryFilterRow(
                selectedCategories = selectedCategories,
                availableCategories = availableCategories,
                onCategoryToggle = onCategoryToggle,
                onClearCategories = onClearCategories,
            )
        }

        if (groups.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (selectedCategories.isNotEmpty())
                            stringResource(R.string.no_matching_groups)
                        else stringResource(R.string.no_duplicates),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            itemsIndexed(
                items = groups,
                key = { _, group -> group.id },
            ) { index, group ->
                val isFirst = index == 0
                val isLast = index == groups.lastIndex
                val shape = when {
                    isFirst && isLast -> RoundedCornerShape(12.dp)
                    isFirst -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                    isLast -> RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        PhotoGroupRow(
                            group = group,
                            onClick = { onGroupClick(group) },
                            onQuickDelete = { onQuickDelete(group) },
                        )
                        if (!isLast) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 88.dp),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingHeaderCard(totalSaving: Long, totalGroupCount: Int, filteredCount: Int) {
    val isFiltered = filteredCount < totalGroupCount
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.potential_saving_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatBytes(totalSaving),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isFiltered) stringResource(R.string.groups_filtered, totalGroupCount, filteredCount)
                    else stringResource(R.string.groups_found, totalGroupCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategories: Set<String>,
    availableCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
) {
    // CATEGORY_LABELS 순서를 유지하면서 실제 결과에 있는 것만 표시
    val orderedCategories = remember(availableCategories) {
        MainViewModel.CATEGORY_LABELS.keys.filter { it in availableCategories }
    }
    if (orderedCategories.isEmpty()) return

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            stringResource(R.string.category_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
    }
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategories.isEmpty(),
                onClick = onClearCategories,
                label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
        items(orderedCategories) { category ->
            FilterChip(
                selected = category in selectedCategories,
                onClick = { onCategoryToggle(category) },
                label = { Text(category, style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

private val THUMB_SIZE = 56.dp

@Composable
private fun PhotoGroupRow(group: PhotoGroup, onClick: () -> Unit, onQuickDelete: () -> Unit) {
    val isVideo = group.type == GroupType.VIDEO_DUPLICATE || group.type == GroupType.SHORT_VIDEO

    // BEST를 항상 첫 번째로 정렬
    val sorted = remember(group.id, group.bestPhotoId) {
        val best = group.photos.find { it.id == group.bestPhotoId }
        val rest = group.photos.filter { it.id != group.bestPhotoId }
        if (best != null) listOf(best) + rest else group.photos
    }
    // 3장 이하: 전부 표시 / 4장 이상: 앞 2장 + "+N" 슬롯
    val showCount = if (sorted.size > 3) 2 else sorted.size
    val overflow = sorted.size - showCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 썸네일 (모두 같은 크기)
        sorted.take(showCount).forEach { photo ->
            GroupThumbnail(photo = photo, isBest = photo.id == group.bestPhotoId)
        }
        // 나머지 개수 슬롯
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(THUMB_SIZE)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+$overflow",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // 절약 용량 (고정)
        Text(
            formatBytes(group.potentialSavingBytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )

        // 퀵 삭제 버튼 (고정)
        IconButton(
            onClick = onQuickDelete,
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                Icons.Filled.Delete,
                contentDescription = stringResource(R.string.quick_delete_desc),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun GroupThumbnail(photo: Photo, isBest: Boolean) {
    Box {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(THUMB_SIZE)
                .clip(RoundedCornerShape(8.dp)),
        )
        if (photo.isVideo) {
            Box(
                modifier = Modifier
                    .size(THUMB_SIZE)
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        if (isBest) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(topStart = 8.dp, bottomEnd = 6.dp),
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
    }
}

