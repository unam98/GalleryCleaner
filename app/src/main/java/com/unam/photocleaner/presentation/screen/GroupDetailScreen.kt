package com.unam.photocleaner.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import coil.compose.AsyncImage
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.domain.model.PhotoGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: PhotoGroup,
    onBack: () -> Unit,
    onDelete: (List<Long>) -> Unit,
) {
    // BEST 제외 나머지 기본 선택
    val selected = remember(group.id) {
        androidx.compose.runtime.mutableStateMapOf<Long, Boolean>().apply {
            group.photos.forEach { put(it.id, it.id != group.bestPhotoId) }
        }
    }

    val selectedIds = selected.entries.filter { it.value }.map { it.key }
    val savingBytes = group.photos.filter { it.id in selectedIds }.sumOf { it.size }

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${group.photos.size}장 그룹") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
            )
        },
        bottomBar = {
            if (selectedIds.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Button(
                        onClick = { onDelete(selectedIds) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Text("${selectedIds.size}장 삭제  •  ${formatBytes(savingBytes)} 절약")
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
                val isBest = photo.id == group.bestPhotoId
                val isSelected = selected[photo.id] == true
                PhotoSelectCell(
                    photo = photo,
                    isBest = isBest,
                    isSelected = isSelected,
                    onToggle = {
                        if (!isBest) selected[photo.id] = !isSelected
                    },
                )
            }
        }
    }
}

@Composable
private fun PhotoSelectCell(
    photo: Photo,
    isBest: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() },
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

        if (!isBest) {
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

        // 파일 크기
        Text(
            formatBytes(photo.size),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(topStart = 6.dp),
                )
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}
