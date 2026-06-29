package com.unam.gallerycleaner.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unam.gallerycleaner.feature.BuildConfig
import com.unam.gallerycleaner.feature.R

@Composable
fun SettingsContent(
    periodicNotification: Boolean,
    screenshotNotification: Boolean,
    onPeriodicNotificationChange: (Boolean) -> Unit,
    onScreenshotNotificationChange: (Boolean) -> Unit,
    onDebugTriggerPeriodicScan: () -> Unit = {},
    onDebugTriggerScreenshotNotif: () -> Unit = {},
    onDebugTriggerScanDoneNotif: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(stringResource(R.string.notification_settings_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        SettingRow(
            title = stringResource(R.string.periodic_notif_title),
            subtitle = stringResource(R.string.periodic_notif_desc),
            checked = periodicNotification,
            onCheckedChange = onPeriodicNotificationChange,
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        SettingRow(
            title = stringResource(R.string.screenshot_notif_title),
            subtitle = stringResource(R.string.screenshot_notif_desc),
            checked = screenshotNotification,
            onCheckedChange = onScreenshotNotificationChange,
        )

        if (BuildConfig.DEBUG) {
            Spacer(Modifier.height(24.dp))
            DebugSection(
                onTriggerPeriodicScan = onDebugTriggerPeriodicScan,
                onTriggerScreenshotNotif = onDebugTriggerScreenshotNotif,
                onTriggerScanDoneNotif = onDebugTriggerScanDoneNotif,
            )
        }
    }
}

@Composable
private fun DebugSection(
    onTriggerPeriodicScan: () -> Unit,
    onTriggerScreenshotNotif: () -> Unit,
    onTriggerScanDoneNotif: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            "🛠 개발자 옵션",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFFE65100),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "DEBUG 빌드 전용 — 릴리즈에서는 표시되지 않음",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFBF360C),
        )
        Spacer(Modifier.height(12.dp))

        DebugButton("주기적 중복 사진 알림 지금 실행", onTriggerPeriodicScan)
        Spacer(Modifier.height(8.dp))
        DebugButton("스크린샷 중요 표시 알림 테스트", onTriggerScreenshotNotif)
        Spacer(Modifier.height(8.dp))
        DebugButton("스캔 완료 알림 테스트", onTriggerScanDoneNotif)
    }
}

@Composable
private fun DebugButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE65100),
            contentColor = Color.White,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    periodicNotification: Boolean,
    screenshotNotification: Boolean,
    onPeriodicNotificationChange: (Boolean) -> Unit,
    onScreenshotNotificationChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        SettingsContent(
            periodicNotification = periodicNotification,
            screenshotNotification = screenshotNotification,
            onPeriodicNotificationChange = onPeriodicNotificationChange,
            onScreenshotNotificationChange = onScreenshotNotificationChange,
            modifier = Modifier.navigationBarsPadding().padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
