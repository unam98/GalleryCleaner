package com.namilab.gallerycleaner.presentation.screen

import com.namilab.gallerycleaner.domain.util.formatBytes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.namilab.gallerycleaner.feature.R
import com.namilab.gallerycleaner.domain.model.GroupType
import com.namilab.gallerycleaner.domain.model.Photo
import com.namilab.gallerycleaner.domain.model.PhotoGroup
import com.namilab.gallerycleaner.presentation.GroupSortOrder
import com.namilab.gallerycleaner.presentation.MainViewModel

@Composable
fun GroupListScreen(
    groups: List<PhotoGroup>,
    totalGroupCount: Int,
    selectedCategories: Set<String>,
    availableCategories: Set<String>,
    sortOrder: GroupSortOrder,
    favoriteIds: Set<Long> = emptySet(),
    onCategoryToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    onSortOrderChange: (GroupSortOrder) -> Unit,
    onGroupClick: (PhotoGroup) -> Unit,
    onQuickDelete: (PhotoGroup) -> Unit,
) {
    val listState = rememberLazyListState()
    var pendingDelete by remember { mutableStateOf<PhotoGroup?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item {
                SavingHeaderCard(
                    displaySaving = groups.sumOf { it.potentialSavingBytes },
                    totalGroupCount = totalGroupCount,
                    filteredCount = groups.size,
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                FilterSortRow(
                    selectedCategories = selectedCategories,
                    availableCategories = availableCategories,
                    onCategoryToggle = onCategoryToggle,
                    onClearCategories = onClearCategories,
                    sortOrder = sortOrder,
                    onSortOrderChange = onSortOrderChange,
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
                                favoriteIds = favoriteIds,
                                onClick = { onGroupClick(group) },
                                onQuickDelete = { pendingDelete = group },
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

        VerticalScrollbar(
            state = listState,
            thumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }

    pendingDelete?.let { group ->
        QuickDeleteConfirmDialog(
            group = group,
            onConfirm = {
                onQuickDelete(group)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun QuickDeleteConfirmDialog(group: PhotoGroup, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val toDelete = remember(group.id, group.bestPhotoId) {
        group.photos.filter { it.id != group.bestPhotoId }
    }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.quick_delete_confirm_title, toDelete.size)) },
        text = {
            Column {
                Text(
                    stringResource(R.string.quick_delete_confirm_desc, formatBytes(group.potentialSavingBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(toDelete, key = { it.id }) { photo ->
                        AsyncImage(
                            model = photo.uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp)),
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.quick_delete_confirm_action), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}


@Composable
private fun VerticalScrollbar(
    state: androidx.compose.foundation.lazy.LazyListState,
    thumbColor: Color,
    modifier: Modifier = Modifier,
) {
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo.size
    if (totalItems <= visibleItems || totalItems == 0) return

    val thumbFraction = visibleItems.toFloat() / totalItems
    val maxScroll = (totalItems - visibleItems).coerceAtLeast(1).toFloat()
    val scrollFraction = state.firstVisibleItemIndex / maxScroll

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(4.dp)
            .padding(vertical = 16.dp),
    ) {
        val thumbHeight = (size.height * thumbFraction).coerceAtLeast(48f)
        val thumbTop = ((size.height - thumbHeight) * scrollFraction.coerceIn(0f, 1f))
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbTop),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(size.width / 2),
        )
    }
}

@Composable
private fun SavingHeaderCard(displaySaving: Long, totalGroupCount: Int, filteredCount: Int) {
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
                    formatBytes(displaySaving),
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
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
private fun FilterSortRow(
    selectedCategories: Set<String>,
    availableCategories: Set<String>,
    onCategoryToggle: (String) -> Unit,
    onClearCategories: () -> Unit,
    sortOrder: GroupSortOrder,
    onSortOrderChange: (GroupSortOrder) -> Unit,
) {
    data class SortChip(val labelRes: Int, val ascending: GroupSortOrder, val descending: GroupSortOrder)

    val orderedCategories = remember(availableCategories) {
        MainViewModel.CATEGORY_LABELS.keys.filter { it in availableCategories }
    }
    val sortChips = remember {
        listOf(
            SortChip(R.string.sort_saving, GroupSortOrder.SAVING_ASC, GroupSortOrder.SAVING_DESC),
            SortChip(R.string.sort_count, GroupSortOrder.COUNT_ASC, GroupSortOrder.COUNT_DESC),
            SortChip(R.string.sort_date, GroupSortOrder.DATE_OLD, GroupSortOrder.DATE_NEW),
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
        item {
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(1.dp)
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
        items(sortChips) { chip ->
            val isAscSelected = sortOrder == chip.ascending
            val isDescSelected = sortOrder == chip.descending
            val isSelected = isAscSelected || isDescSelected
            FilterChip(
                selected = isSelected,
                onClick = { onSortOrderChange(if (isDescSelected) chip.ascending else chip.descending) },
                label = { Text(stringResource(chip.labelRes), style = MaterialTheme.typography.labelLarge) },
                trailingIcon = if (isSelected) {
                    {
                        Icon(
                            imageVector = if (isAscSelected) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                } else null,
                shape = RoundedCornerShape(8.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = Color.White,
                    selectedTrailingIconColor = Color.White,
                ),
            )
        }
    }
}

private val THUMB_SIZE = 72.dp

@Composable
private fun PhotoGroupRow(group: PhotoGroup, favoriteIds: Set<Long>, onClick: () -> Unit, onQuickDelete: () -> Unit) {
    val sorted = remember(group.id, group.bestPhotoId) {
        val best = group.photos.find { it.id == group.bestPhotoId }
        val rest = group.photos.filter { it.id != group.bestPhotoId }
        if (best != null) listOf(best) + rest else group.photos
    }
    // 리딩 구간을 "항상 정확히 2장"으로 고정폭 처리 — 넘치는 개수는 별도 박스를 더 붙이는 대신
    // 마지막 썸네일 위에 반투명 스크림+"+N" 오버레이로 표시한다. 이러면 리딩 구간 너비가 뱃지
    // 유무와 무관하게 항상 동일해서, 뒤따르는 용량 텍스트·삭제 아이콘이 화면 폭에 밀려 찌그러질
    // 여지가 구조적으로 없고, 오른쪽 끝 삭제 아이콘 위치도 행마다 항상 동일하게 고정된다.
    val showCount = minOf(sorted.size, 2)
    val overflow = sorted.size - showCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        sorted.take(showCount).forEachIndexed { index, photo ->
            GroupThumbnail(
                photo = photo,
                isBest = photo.id == group.bestPhotoId,
                isFavorite = photo.id in favoriteIds,
                overflowCount = if (index == showCount - 1) overflow else 0,
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            formatBytes(group.potentialSavingBytes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    onClick = onQuickDelete,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.quick_delete_desc),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun GroupThumbnail(photo: Photo, isBest: Boolean, isFavorite: Boolean = false, overflowCount: Int = 0) {
    Box {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(THUMB_SIZE)
                .clip(RoundedCornerShape(8.dp)),
        )
        if (overflowCount > 0) {
            // 남은 개수 오버레이가 재생 아이콘보다 우선 — 동시에 필요할 일은 없다 (마지막 썸네일 하나에만 붙음)
            Box(
                modifier = Modifier
                    .size(THUMB_SIZE)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "+$overflowCount",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        } else if (photo.isVideo) {
            Box(
                modifier = Modifier
                    .size(THUMB_SIZE)
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
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
                    Icons.Rounded.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
        if (isFavorite) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        MaterialTheme.colorScheme.tertiary,
                        RoundedCornerShape(topStart = 6.dp, bottomEnd = 8.dp),
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Icon(
                    Icons.Rounded.GppGood,
                    contentDescription = stringResource(R.string.tab_favorites),
                    tint = Color.White,
                    modifier = Modifier.size(9.dp),
                )
            }
        }
    }
}
