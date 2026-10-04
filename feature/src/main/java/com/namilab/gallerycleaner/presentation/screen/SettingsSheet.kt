package com.namilab.gallerycleaner.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.namilab.gallerycleaner.feature.BuildConfig
import com.namilab.gallerycleaner.presentation.ui.theme.DebugWarningBg
import com.namilab.gallerycleaner.presentation.ui.theme.DebugWarningSubtitle
import com.namilab.gallerycleaner.presentation.ui.theme.DebugWarningTitle
import com.namilab.gallerycleaner.presentation.ui.theme.GoldAccent
import com.namilab.gallerycleaner.presentation.ui.theme.PremiumCheckGold
import com.namilab.gallerycleaner.presentation.ui.theme.PremiumGradientEnd
import com.namilab.gallerycleaner.presentation.ui.theme.PremiumGradientStart
import com.namilab.gallerycleaner.feature.R

@Composable
fun SettingsContent(
    periodicNotification: Boolean,
    screenshotNotification: Boolean,
    onPeriodicNotificationChange: (Boolean) -> Unit,
    onScreenshotNotificationChange: (Boolean) -> Unit,
    isPremium: Boolean = false,
    onPurchasePremium: () -> Unit = {},
    onDebugTriggerPeriodicScan: () -> Unit = {},
    onDebugTriggerScreenshotNotif: () -> Unit = {},
    onDebugTriggerScanDoneNotif: () -> Unit = {},
    onDebugTogglePremium: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        PremiumCard(isPremium = isPremium, onPurchase = onPurchasePremium)

        Spacer(Modifier.height(24.dp))
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
                isPremium = isPremium,
                onTriggerPeriodicScan = onDebugTriggerPeriodicScan,
                onTriggerScreenshotNotif = onDebugTriggerScreenshotNotif,
                onTriggerScanDoneNotif = onDebugTriggerScanDoneNotif,
                onTogglePremium = onDebugTogglePremium,
            )
        }
    }
}

@Composable
private fun PremiumCard(isPremium: Boolean, onPurchase: () -> Unit) {
    if (isPremium) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column {
                    Text(
                        "프리미엄 이용 중",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        "광고 없이 모든 기능을 무제한으로 사용할 수 있어요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(listOf(PremiumGradientStart, PremiumGradientEnd)),
                        RoundedCornerShape(16.dp),
                    )
                    .padding(20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Star,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "프리미엄",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.height(8.dp))
                listOf(
                    "광고 없이 무제한 스캔·정리",
                    "전체 기간 스캔 잠금 해제",
                ).forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = PremiumCheckGold,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(benefit, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onPurchase,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = PremiumGradientStart,
                    ),
                ) {
                    Text("구매하기", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DebugSection(
    isPremium: Boolean,
    onTriggerPeriodicScan: () -> Unit,
    onTriggerScreenshotNotif: () -> Unit,
    onTriggerScanDoneNotif: () -> Unit,
    onTogglePremium: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DebugWarningBg, RoundedCornerShape(12.dp))
            .padding(16.dp),
    ) {
        Text(
            "🛠 개발자 옵션",
            style = MaterialTheme.typography.labelLarge,
            color = DebugWarningTitle,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "DEBUG 빌드 전용 — 릴리즈에서는 표시되지 않음",
            style = MaterialTheme.typography.bodySmall,
            color = DebugWarningSubtitle,
        )
        Spacer(Modifier.height(12.dp))

        DebugButton("주기적 중복 사진 알림 지금 실행", onTriggerPeriodicScan)
        Spacer(Modifier.height(8.dp))
        DebugButton("스크린샷 중요 표시 알림 테스트", onTriggerScreenshotNotif)
        Spacer(Modifier.height(8.dp))
        DebugButton("스캔 완료 알림 테스트", onTriggerScanDoneNotif)
        Spacer(Modifier.height(8.dp))
        DebugButton(
            if (isPremium) "프리미엄 해제 (테스트용)" else "프리미엄 활성화 (테스트용)",
            onTogglePremium,
        )
    }
}

@Composable
private fun DebugButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DebugWarningTitle,
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
