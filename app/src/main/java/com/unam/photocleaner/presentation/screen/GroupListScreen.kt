package com.unam.photocleaner.presentation.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.unam.photocleaner.R
import com.unam.photocleaner.domain.model.GroupType
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.presentation.MainViewModel
import com.unam.photocleaner.presentation.ui.theme.iOSGreen
import com.unam.photocleaner.presentation.ui.theme.iOSOrange

@Composable
fun GroupListScreen(
    groups: List<PhotoGroup>,
    totalSaving: Long,
    totalGroupCount: Int,
    selectedCategory: String?,
    keyword: String,
    onCategorySelect: (String?) -> Unit,
    onKeywordChange: (String) -> Unit,
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
                selectedCategory = selectedCategory,
                onCategorySelect = onCategorySelect,
            )
        }
        item {
            KeywordSearchField(keyword = keyword, onKeywordChange = onKeywordChange)
            Spacer(Modifier.height(8.dp))
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
                        if (selectedCategory != null || keyword.isNotBlank())
                            stringResource(R.string.no_matching_groups)
                        else stringResource(R.string.no_duplicates),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        } else {
            // 흰 카드 안에 그룹 목록 (iOS grouped style)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                ) {
                    Column {
                        groups.forEachIndexed { index, group ->
                            PhotoGroupRow(
                                group = group,
                                onClick = { onGroupClick(group) },
                                onQuickDelete = { onQuickDelete(group) },
                            )
                            if (index < groups.lastIndex) {
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
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
) {
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
                selected = selectedCategory == null,
                onClick = { onCategorySelect(null) },
                label = { Text(stringResource(R.string.filter_all), style = MaterialTheme.typography.labelLarge) },
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
        items(MainViewModel.CATEGORY_LABELS.keys.toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(if (selectedCategory == category) null else category) },
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

@Composable
private fun KeywordSearchField(keyword: String, onKeywordChange: (String) -> Unit) {
    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        placeholder = {
            Text(
                stringResource(R.string.keyword_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = { onKeywordChange("") }) {
                    Icon(Icons.Outlined.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    )
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

        // 카운트 + 절약 용량 (고정)
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isVideo) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    stringResource(
                        if (isVideo) R.string.group_title_video else R.string.group_title_photo,
                        group.photos.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                formatBytes(group.potentialSavingBytes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

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

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
