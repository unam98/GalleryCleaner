package com.unam.photocleaner.presentation.screen

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unam.photocleaner.R
import com.unam.photocleaner.domain.model.GroupType
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.domain.model.PhotoGroup
import com.unam.photocleaner.presentation.MainViewModel

@Composable
fun GroupListScreen(
    groups: List<PhotoGroup>,
    totalSaving: Long,
    totalGroupCount: Int,
    isLabeling: Boolean,
    selectedCategory: String?,
    keyword: String,
    onCategorySelect: (String?) -> Unit,
    onKeywordChange: (String) -> Unit,
    onGroupClick: (PhotoGroup) -> Unit,
    onQuickDelete: (PhotoGroup) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SavingSummaryCard(
            totalSaving = totalSaving,
            totalGroupCount = totalGroupCount,
            filteredCount = groups.size,
        )
        CategoryFilterRow(
            isLabeling = isLabeling,
            selectedCategory = selectedCategory,
            onCategorySelect = onCategorySelect,
        )
        KeywordSearchField(
            keyword = keyword,
            onKeywordChange = onKeywordChange,
        )
        if (groups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (selectedCategory != null || keyword.isNotBlank()) stringResource(R.string.no_matching_groups)
                    else stringResource(R.string.no_duplicates),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn {
                items(groups, key = { it.id }) { group ->
                    PhotoGroupRow(
                        group = group,
                        onClick = { onGroupClick(group) },
                        onQuickDelete = { onQuickDelete(group) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SavingSummaryCard(totalSaving: Long, totalGroupCount: Int, filteredCount: Int) {
    val isFiltered = filteredCount < totalGroupCount
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.potential_saving_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                formatBytes(totalSaving),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (isFiltered) stringResource(R.string.groups_filtered, totalGroupCount, filteredCount)
                else stringResource(R.string.groups_found, totalGroupCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun CategoryFilterRow(
    isLabeling: Boolean,
    selectedCategory: String?,
    onCategorySelect: (String?) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
    ) {
        Text(
            stringResource(R.string.category_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isLabeling) {
            Spacer(Modifier.width(6.dp))
            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                stringResource(R.string.analyzing),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
                enabled = !isLabeling,
                label = { Text(stringResource(R.string.filter_all)) },
            )
        }
        items(MainViewModel.CATEGORY_LABELS.keys.toList()) { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelect(if (selectedCategory == category) null else category) },
                enabled = !isLabeling,
                label = { Text(category) },
            )
        }
    }
}

@Composable
private fun KeywordSearchField(keyword: String, onKeywordChange: (String) -> Unit) {
    OutlinedTextField(
        value = keyword,
        onValueChange = onKeywordChange,
        placeholder = { Text(stringResource(R.string.keyword_hint)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (keyword.isNotEmpty()) {
                IconButton(onClick = { onKeywordChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.clear))
                }
            }
        },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun PhotoGroupRow(group: PhotoGroup, onClick: () -> Unit, onQuickDelete: () -> Unit) {
    val isVideo = group.type == GroupType.VIDEO_DUPLICATE || group.type == GroupType.SHORT_VIDEO
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        group.photos.take(4).forEach { photo ->
            PhotoThumbnail(photo = photo, isBest = photo.id == group.bestPhotoId)
        }
        if (group.photos.size > 4) OverflowCount(count = group.photos.size - 4)
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isVideo) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(2.dp))
                }
                Text(
                    stringResource(
                        if (isVideo) R.string.group_title_video else R.string.group_title_photo,
                        group.photos.size,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(formatBytes(group.potentialSavingBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        // BEST 제외 즉시 삭제 버튼
        IconButton(onClick = onQuickDelete) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.quick_delete_desc),
                tint = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PhotoThumbnail(photo: Photo, isBest: Boolean) {
    Box {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isBest) 2.dp else 0.dp,
                    color = if (isBest) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                ),
        )
        if (isBest) {
            Text(
                "BEST",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
            )
        }
    }
}

@Composable
private fun OverflowCount(count: Int) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text("+$count", style = MaterialTheme.typography.bodyLarge)
    }
}

fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}
