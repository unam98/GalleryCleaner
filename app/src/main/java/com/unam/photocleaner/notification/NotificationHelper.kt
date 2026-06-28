package com.unam.photocleaner.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.unam.photocleaner.MainActivity
import com.unam.photocleaner.presentation.screen.formatBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SCAN, "사진 정리 알림", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "유사·중복 사진 발견 시 알림" }
            )
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_SCREENSHOT, "스크린샷 중요 표시", NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = "스크린샷 저장 시 중요 여부 묻는 알림" }
            )
        }
    }

    private fun hasNotifyPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    fun showDuplicateFound(groupCount: Int, savingBytes: Long, sinceMs: Long) {
        if (!hasNotifyPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(MainActivity.EXTRA_AUTO_SCAN, true)
            putExtra(MainActivity.EXTRA_SCAN_SINCE_MS, sinceMs)
        }
        val pi = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val body = if (savingBytes > 0) "${groupCount}그룹 발견 · ${formatBytes(savingBytes)} 정리 가능해요"
                   else "${groupCount}그룹의 유사 사진이 발견됐어요"

        NotificationManagerCompat.from(context).notify(
            NOTIF_SCAN,
            NotificationCompat.Builder(context, CHANNEL_SCAN)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("유사 사진 발견")
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showScreenshotFavoritePrompt(photoId: Long, displayName: String) {
        if (!hasNotifyPermission()) return

        val favoriteIntent = Intent(context, FavoriteActionReceiver::class.java).apply {
            action = FavoriteActionReceiver.ACTION_MARK_FAVORITE
            putExtra(FavoriteActionReceiver.EXTRA_PHOTO_ID, photoId)
        }
        val favoritePi = PendingIntent.getBroadcast(
            context, photoId.toInt(), favoriteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val name = displayName.ifEmpty { "스크린샷" }
        NotificationManagerCompat.from(context).notify(
            NOTIF_SCREENSHOT,
            NotificationCompat.Builder(context, CHANNEL_SCREENSHOT)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("스크린샷 저장됨")
                .setContentText("\"$name\" — 중요 사진으로 설정할까요?")
                .addAction(0, "⭐ 중요로 설정", favoritePi)
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val CHANNEL_SCAN = "photo_cleaner_duplicates"
        const val CHANNEL_SCREENSHOT = "photo_cleaner_screenshot"
        const val NOTIF_SCAN = 1001
        const val NOTIF_SCREENSHOT = 1002
    }
}
