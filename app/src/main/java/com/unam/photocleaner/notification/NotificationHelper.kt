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
import com.unam.photocleaner.R
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
                NotificationChannel(
                    CHANNEL_SCAN,
                    context.getString(R.string.notif_channel_scan_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.notif_channel_scan_desc) }
            )
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SCREENSHOT,
                    context.getString(R.string.notif_channel_screenshot_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = context.getString(R.string.notif_channel_screenshot_desc) }
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

        val body = if (savingBytes > 0) context.getString(R.string.notif_scan_body_saving, groupCount, formatBytes(savingBytes))
                   else context.getString(R.string.notif_scan_body_no_saving, groupCount)

        NotificationManagerCompat.from(context).notify(
            NOTIF_SCAN,
            NotificationCompat.Builder(context, CHANNEL_SCAN)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle(context.getString(R.string.notif_scan_title))
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showScreenshotFavoritePrompt(photoId: Long, displayName: String) {
        if (!hasNotifyPermission()) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPi = PendingIntent.getActivity(
            context, NOTIF_SCREENSHOT, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val favoriteIntent = Intent(context, FavoriteActionReceiver::class.java).apply {
            action = FavoriteActionReceiver.ACTION_MARK_FAVORITE
            putExtra(FavoriteActionReceiver.EXTRA_PHOTO_ID, photoId)
            putExtra(FavoriteActionReceiver.EXTRA_DISPLAY_NAME, displayName)
        }
        val favoritePi = PendingIntent.getBroadcast(
            context, photoId.toInt(), favoriteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val name = displayName.ifEmpty { context.getString(R.string.screenshot_default_name) }
        NotificationManagerCompat.from(context).notify(
            NOTIF_SCREENSHOT,
            NotificationCompat.Builder(context, CHANNEL_SCREENSHOT)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle(context.getString(R.string.notif_screenshot_title))
                .setContentText(context.getString(R.string.notif_screenshot_body, name))
                .setContentIntent(openAppPi)
                .addAction(0, context.getString(R.string.notif_screenshot_action), favoritePi)
                .setAutoCancel(true)
                .build(),
        )
    }

    fun showScreenshotMarkedConfirm(displayName: String) {
        if (!hasNotifyPermission()) return
        val name = displayName.ifEmpty { context.getString(R.string.screenshot_default_name) }
        NotificationManagerCompat.from(context).notify(
            NOTIF_SCREENSHOT,
            NotificationCompat.Builder(context, CHANNEL_SCREENSHOT)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle(context.getString(R.string.notif_screenshot_marked))
                .setContentText(name)
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
