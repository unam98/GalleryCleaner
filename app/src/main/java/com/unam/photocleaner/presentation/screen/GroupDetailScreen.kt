package com.unam.photocleaner.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unam.photocleaner.R
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.domain.model.PhotoGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: PhotoGroup,
    favoriteIds: Set<Long>,
    onBack: () -> Unit,
    onDelete: (List<Photo>) -> Unit,
    onToggleFavorite: (Long) -> Unit,
) {
    val selected = remember(group.id) {
        androidx.compose.runtime.mutableStateMapOf<Long, Boolean>().apply {
            // BEST도 선택 가능 — 초기값 false (권장: 유지), 즐겨찾기는 false (삭제 불가)
            group.photos.forEach { put(it.id, it.id != group.bestPhotoId && it.id !in favoriteIds) }
        }
    }
    remember(favoriteIds) {
        group.photos.filter { it.id in favoriteIds }.forEach { selected[it.id] = false }
    }

    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }

    val selectedPhotos = group.photos.filter { selected[it.id] == true && it.id !in favoriteIds }
    val savingBytes = selectedPhotos.sumOf { it.size }
    val isVideo = group.photos.firstOrNull()?.isVideo == true

    // 풀스크린 뷰어가 열려 있으면 뒤로가기로 먼저 닫기
    BackHandler(enabled = fullScreenIndex != null) { fullScreenIndex = null }
    BackHandler(enabled = fullScreenIndex == null, onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isVideo) R.string.group_title_video else R.string.group_title_photo, group.photos.size)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            if (selectedPhotos.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { onDelete(selectedPhotos) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text(stringResource(if (isVideo) R.string.delete_btn_video else R.string.delete_btn_photo, selectedPhotos.size, formatBytes(savingBytes)))
                    }
                }
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(group.photos, key = { it.id }) { photo ->
                val index = group.photos.indexOf(photo)
                val isBest = photo.id == group.bestPhotoId
                val isFavorite = photo.id in favoriteIds
                val isSelected = selected[photo.id] == true
                PhotoSelectCell(
                    photo = photo,
                    isBest = isBest,
                    isFavorite = isFavorite,
                    isSelected = isSelected,
                    onViewFull = { fullScreenIndex = index },
                    onToggle = { if (!isFavorite) selected[photo.id] = !isSelected },
                    onToggleFavorite = { onToggleFavorite(photo.id) },
                )
            }
        }
    }

    // 풀스크린 뷰어 오버레이
    if (fullScreenIndex != null) {
        PhotoFullScreenViewer(
            photos = group.photos,
            initialIndex = fullScreenIndex!!,
            bestPhotoId = group.bestPhotoId,
            favoriteIds = favoriteIds,
            isSelected = { id -> selected[id] == true },
            onToggle = { id ->
                if (id != group.bestPhotoId && id !in favoriteIds) selected[id] = !(selected[id] ?: false)
            },
            onToggleFavorite = onToggleFavorite,
            onDismiss = { fullScreenIndex = null },
        )
    }
}

@Composable
private fun PhotoFullScreenViewer(
    photos: List<Photo>,
    initialIndex: Int,
    bestPhotoId: Long,
    favoriteIds: Set<Long>,
    isSelected: (Long) -> Boolean,
    onToggle: (Long) -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex) { photos.size }
    val current = photos[pagerState.currentPage]
    val isBest = current.id == bestPhotoId
    val isFavorite = current.id in favoriteIds
    val selected = isSelected(current.id)

    // 핀치줌 상태 (페이지 이동 시 리셋)
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(pagerState.currentPage) { scale = 1f; offset = Offset.Zero }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + panChange else Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 사진/동영상 썸네일
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = scale <= 1f,  // 확대 중엔 스와이프 비활성화
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    model = photos[page].uri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (page == pagerState.currentPage) scale else 1f,
                            scaleY = if (page == pagerState.currentPage) scale else 1f,
                            translationX = if (page == pagerState.currentPage) offset.x else 0f,
                            translationY = if (page == pagerState.currentPage) offset.y else 0f,
                        )
                        .transformable(
                            state = transformableState,
                            enabled = page == pagerState.currentPage,
                        )
                        .pointerInput(page) {
                            detectTapGestures(onDoubleTap = {
                                if (page == pagerState.currentPage) {
                                    if (scale > 1f) { scale = 1f; offset = Offset.Zero }
                                    else scale = 2.5f
                                }
                            })
                        },
                )
                // 동영상 표시
                if (photos[page].isVideo) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center),
                    )
                }
            }
        }

        // 상단 바
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
            if (isBest) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        "BEST",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            } else {
                Spacer(Modifier)
            }
            IconButton(onClick = { onToggleFavorite(current.id) }) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (isFavorite) stringResource(R.string.favorite_remove) else stringResource(R.string.favorite_add),
                    tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else Color.White,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
        }

        // 하단 정보 + 삭제 토글
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row {
                Text(formatBytes(current.size), color = Color.White, style = MaterialTheme.typography.bodySmall)
                if (current.isVideo && current.duration > 0) {
                    Spacer(Modifier.size(8.dp))
                    Text(formatDuration(current.duration), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!isFavorite) {
                Button(
                    onClick = { onToggle(current.id) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.error
                                         else Color.White.copy(alpha = 0.2f),
                    ),
                ) {
                    Text(
                        if (selected) stringResource(R.string.delete_selected)
                        else if (isBest) stringResource(R.string.best_not_selected)
                        else stringResource(R.string.delete_not_selected),
                        color = Color.White,
                    )
                }
            } else {
                Text(stringResource(R.string.favorite_protected), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun PhotoSelectCell(
    photo: Photo,
    isBest: Boolean,
    isFavorite: Boolean,
    isSelected: Boolean,
    onViewFull: () -> Unit,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onViewFull() },   // 사진 탭 → 풀스크린
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(if (isSelected) Modifier.alpha(0.55f) else Modifier),
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f)),
            )
        }

        // 체크박스 (즐겨찾기만 비활성화, BEST는 허용)
        if (!isFavorite) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                modifier = Modifier.align(Alignment.TopEnd),
                colors = CheckboxDefaults.colors(
                    checkmarkColor = Color.White,
                    checkedColor = MaterialTheme.colorScheme.error,
                    uncheckedColor = Color.White,
                ),
            )
        }

        if (isBest) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    "BEST",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }

        // 동영상 재생 아이콘 (중앙)
        if (photo.isVideo) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier
                    .size(36.dp)
                    .align(Alignment.Center),
            )
        }

        // 즐겨찾기 별 아이콘 (좌하단)
        IconButton(
            onClick = onToggleFavorite,
            modifier = Modifier.align(Alignment.BottomStart).padding(2.dp),
        ) {
            Icon(
                if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = if (isFavorite) MaterialTheme.colorScheme.tertiary else Color.White.copy(alpha = 0.8f),
            )
        }

        Text(
            formatBytes(photo.size),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(topStart = 6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}
