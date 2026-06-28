package com.unam.photocleaner.presentation.screen

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.unam.photocleaner.presentation.MainEvent
import com.unam.photocleaner.presentation.MainViewModel
import com.unam.photocleaner.presentation.UiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()

    val permissionName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val permission = rememberPermissionState(permissionName)

    // Android 10+ 시스템 삭제 다이얼로그 런처
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onSystemDeleteConfirmed()
        }
    }

    // 시스템 삭제 다이얼로그 이벤트 수신
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.RequestSystemDelete -> {
                    deleteLauncher.launch(
                        IntentSenderRequest.Builder(event.intentSender).build()
                    )
                }
            }
        }
    }

    // 그룹 상세 화면
    if (selectedGroup != null) {
        GroupDetailScreen(
            group = selectedGroup!!,
            onBack = { viewModel.clearGroupSelection() },
            onDelete = { ids -> viewModel.requestDelete(ids) },
        )
        return
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PhotoCleaner") }) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (val s = state) {
                is UiState.Idle -> {
                    Button(onClick = {
                        if (permission.status.isGranted) viewModel.scan()
                        else permission.launchPermissionRequest()
                    }) {
                        Text(if (permission.status.isGranted) "스캔 시작" else "사진 접근 허용")
                    }
                }

                is UiState.Scanning -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("사진 분석 중...")
                    }
                }

                is UiState.Done -> GroupListScreen(
                    groups = s.groups,
                    totalSaving = s.totalSavingBytes,
                    onGroupClick = { group -> viewModel.selectGroup(group) },
                )

                is UiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("오류: ${s.message}")
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.reset() }) { Text("다시 시도") }
                    }
                }
            }
        }
    }
}
