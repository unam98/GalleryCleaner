package com.unam.gallerycleaner.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class ScanForegroundService : Service() {

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val total = intent?.getIntExtra(EXTRA_TOTAL, 0) ?: 0
        ensureChannel()
        val notification = buildNotification(total)
        ServiceCompat.startForeground(
            this, NOTIF_ID, notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        return START_NOT_STICKY
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "사진 스캔 진행", NotificationManager.IMPORTANCE_LOW)
                        .apply { setShowBadge(false) }
                )
            }
        }
    }

    private fun buildNotification(total: Int): Notification {
        val body = if (total > 0) "총 ${total}장 분석 중 · 앱을 나가도 계속 진행돼요"
                   else "앱을 나가도 계속 진행돼요"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_gallery)
            .setContentTitle("사진 분석 중…")
            .setContentText(body)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "gallery_cleaner_scan_progress"
        const val NOTIF_ID = 2001
        private const val ACTION_STOP = "com.unam.gallerycleaner.STOP_SCAN"
        private const val EXTRA_TOTAL = "total"

        fun start(context: Context, total: Int = 0) {
            val intent = Intent(context, ScanForegroundService::class.java)
                .putExtra(EXTRA_TOTAL, total)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ScanForegroundService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}
