package com.namilab.gallerycleaner.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
        ensureChannel(this)
        val notification = buildNotification(this, current = 0, total = total)
        ServiceCompat.startForeground(
            this, NOTIF_ID, notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0,
        )
        return START_NOT_STICKY
    }

    companion object {
        private const val CHANNEL_ID = "gallery_cleaner_scan_progress"
        const val NOTIF_ID = 2001
        private const val ACTION_STOP = "com.namilab.gallerycleaner.STOP_SCAN"
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

        /** 진행 상황에 따라 알림 프로그레스 바를 갱신한다. 서비스 외부(ViewModel)에서 직접 호출. */
        fun update(context: Context, current: Int, total: Int, etaMs: Long) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            nm.notify(NOTIF_ID, buildNotification(context, current, total, etaMs))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, ScanForegroundService::class.java).apply { action = ACTION_STOP }
            )
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    ?: return
                if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                    nm.createNotificationChannel(
                        NotificationChannel(CHANNEL_ID, "사진 스캔 진행", NotificationManager.IMPORTANCE_LOW)
                            .apply { setShowBadge(false) }
                    )
                }
            }
        }

        private fun buildNotification(context: Context, current: Int, total: Int, etaMs: Long = 0L): Notification {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP }
            val contentIntent = launchIntent?.let {
                PendingIntent.getActivity(
                    context, 0, it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
            val isIndeterminate = total == 0
            val percent = if (total > 0) (current * 100 / total) else 0
            val etaSuffix = when {
                etaMs <= 0 || current <= 0 || current >= total -> ""
                etaMs < 60_000 -> " · 약 ${(etaMs / 1000).coerceAtLeast(1)}초 남았어요"
                else -> " · 약 ${((etaMs + 30_000) / 60_000).coerceAtLeast(1)}분 남았어요"
            }
            val body = if (isIndeterminate) "앱을 나가도 계속 진행돼요" else "${percent}%$etaSuffix"
            return NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_gallery)
                .setContentTitle("사진 분석 중…")
                .setContentText(body)
                .setProgress(total, current, isIndeterminate)
                .setOngoing(true)
                .setSilent(true)
                .apply { contentIntent?.let { setContentIntent(it) } }
                .build()
        }
    }
}
