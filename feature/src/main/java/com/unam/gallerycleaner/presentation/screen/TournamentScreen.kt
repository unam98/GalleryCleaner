package com.unam.gallerycleaner.presentation.screen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.unam.gallerycleaner.domain.model.Photo
import com.unam.gallerycleaner.domain.util.formatBytes
import com.unam.gallerycleaner.feature.R
import com.unam.gallerycleaner.presentation.TournamentState

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
            .padding(top = 16.dp, bottom = 32.dp),
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

        Spacer(Modifier.height(16.dp))

        Text(
            stringResource(R.string.tournament_instruction),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // 대결 카드들
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhotoCard(
                photo = left,
                onClick = { onPick(left) },
                modifier = Modifier.weight(1f),
            )

            // VS 구분선
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "VS",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                )
            }

            if (right != null) {
                PhotoCard(
                    photo = right,
                    onClick = { onPick(right) },
                    modifier = Modifier.weight(1f),
                )
            } else {
                // 홀수 — 빈 자리 표시
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
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
        }
    }
}

@Composable
private fun PhotoCard(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 사진 정보 오버레이 (하단)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)),
                    )
                )
                .padding(8.dp),
        ) {
            Text(
                formatBytes(photo.size),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
        // 탭 힌트 오버레이
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
        // 헤더
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

        // 우승 사진
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 32.dp)
                .clip(RoundedCornerShape(16.dp)),
        ) {
            AsyncImage(
                model = winner.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            // 트로피 뱃지
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color(0xFFFFD700), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "🏆 BEST",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            formatBytes(winner.size),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(20.dp))

        // 액션 버튼들
        Button(
            onClick = onKeepWinner,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                stringResource(R.string.tournament_keep_winner, totalPhotos - 1),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(R.string.tournament_close_no_delete), color = Color.White)
        }
    }
}
