package com.namilab.gallerycleaner.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.namilab.gallerycleaner.domain.model.Photo
import com.namilab.gallerycleaner.domain.util.formatBytes
import com.namilab.gallerycleaner.feature.R
import com.namilab.gallerycleaner.presentation.TournamentState

@Composable
fun TournamentScreen(
    state: TournamentState,
    onPick: (Photo) -> Unit,
    onClose: () -> Unit,
    onKeepWinner: (Photo) -> Unit,
) {
    BackHandler { onClose() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AnimatedContent(
            targetState = state.isComplete,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tournament_content",
        ) { isComplete ->
            if (isComplete) {
                val winner = state.winner!!
                TournamentResultScreen(
                    winner = winner,
                    totalPhotos = state.totalPhotos,
                    onKeepWinner = { onKeepWinner(winner) },
                    onClose = onClose,
                )
            } else {
                TournamentMatchScreen(
                    state = state,
                    onPick = onPick,
                    onClose = onClose,
                )
            }
        }
    }
}

@Composable
private fun TournamentMatchScreen(
    state: TournamentState,
    onPick: (Photo) -> Unit,
    onClose: () -> Unit,
) {
    val left = state.left ?: return
    val right = state.right

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 헤더
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
            Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(R.string.tournament_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(R.string.tournament_round, state.roundNumber, state.totalRounds),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Text(
            stringResource(R.string.tournament_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))

        // 대결 카드 — 세로 배치 고정
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            PhotoCard(photo = left, onClick = { onPick(left) }, modifier = Modifier.fillMaxWidth().weight(1f))
            Box(modifier = Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.Center) {
                Text("VS", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            }
            if (right != null) {
                PhotoCard(photo = right, onClick = { onPick(right) }, modifier = Modifier.fillMaxWidth().weight(1f))
            } else {
                AutoAdvanceSlot(modifier = Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun AutoAdvanceSlot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.tournament_auto_advance),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var boxWidth by remember { mutableStateOf(0) }
    var boxHeight by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .onSizeChanged { boxWidth = it.width; boxHeight = it.height }
            // clickable 유지 → ripple 피드백 + 탭 선택 처리
            .clickable {
                if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f }
                else onClick()
            }
            // 핀치/드래그 제스처 — clickable과 공존 (touchSlop 초과 시에만 발동)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = newScale
                    if (newScale > 1f) {
                        // 이미지가 박스 경계를 벗어나지 않도록 클램핑
                        // 확대 시 여분 영역: (scale - 1) * size / 2
                        val maxX = boxWidth * (newScale - 1f) / 2f
                        val maxY = boxHeight * (newScale - 1f) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    } else {
                        offsetX = 0f; offsetY = 0f
                    }
                }
            },
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale; scaleY = scale
                    translationX = offsetX; translationY = offsetY
                },
        )
        // 하단 파일크기 오버레이
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
                .padding(8.dp),
        ) {
            Text(formatBytes(photo.size), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
        }
        // 확대 중이 아닐 때만 "선택" 힌트 표시
        if (scale <= 1f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    stringResource(R.string.tournament_pick_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun TournamentResultScreen(
    winner: Photo,
    totalPhotos: Int,
    onKeepWinner: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)) {
                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
            Text(
                stringResource(R.string.tournament_result_title),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.tournament_result_desc, totalPhotos),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
        )

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            AsyncImage(model = winner.uri, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0xFFFFD700), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text("🏆 BEST", style = MaterialTheme.typography.labelMedium, color = Color.Black, fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(formatBytes(winner.size), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onKeepWinner,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.tournament_keep_winner, totalPhotos - 1), color = Color.Black, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.tournament_close_no_delete), color = Color.White)
        }
    }
}
