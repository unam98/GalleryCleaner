package com.unam.photocleaner.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.unam.photocleaner.R
import com.unam.photocleaner.domain.model.Photo
import com.unam.photocleaner.presentation.UiState
import com.unam.photocleaner.presentation.ui.theme.iOSOrange

@Composable
fun FavoritesScreen(
    scanState: UiState,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
) {
    val favoritePhotos = remember(scanState, favoriteIds) {
        when (scanState) {
            is UiState.Done -> scanState.groups
                .flatMap { it.photos }
                .distinctBy { it.id }
                .filter { it.id in favoriteIds }
            else -> emptyList()
        }
    }

    var fullScreenIndex by remember(favoritePhotos) { mutableStateOf<Int?>(null) }

    if (favoritePhotos.isEmpty()) {
        FavoritesEmptyState(hasScanned = scanState is UiState.Done)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(favoritePhotos, key = { _, p -> p.id }) { index, photo ->
                FavoriteCell(
                    photo = photo,
                    onClick = { fullScreenIndex = index },
                )
            }
        }
    }

    if (fullScreenIndex != null) {
        PhotoFullScreenViewer(
            photos = favoritePhotos,
            initialIndex = fullScreenIndex!!,
            bestPhotoId = -1L,
            favoriteIds = favoriteIds,
            isSelected = { false },
            onToggle = {},
            onToggleFavorite = onToggleFavorite,
            onDismiss = { fullScreenIndex = null },
        )
    }
}

@Composable
private fun FavoriteCell(photo: Photo, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() },
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (photo.isVideo) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = iOSOrange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(14.dp),
        )
    }
}

@Composable
private fun FavoritesEmptyState(hasScanned: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            if (hasScanned) stringResource(R.string.favorites_empty)
            else stringResource(R.string.favorites_no_scan),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp),
        )
    }
}
