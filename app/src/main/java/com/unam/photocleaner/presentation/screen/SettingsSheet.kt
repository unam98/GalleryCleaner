package com.unam.photocleaner.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.unam.photocleaner.R

@Composable
fun SettingsContent(
    periodicNotification: Boolean,
    screenshotNotification: Boolean,
    onPeriodicNotificationChange: (Boolean) -> Unit,
    onScreenshotNotificationChange: (Boolean) -> Unit,
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
