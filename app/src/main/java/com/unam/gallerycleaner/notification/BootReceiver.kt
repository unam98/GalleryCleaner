package com.unam.gallerycleaner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unam.gallerycleaner.data.local.AppPreferences
import com.unam.gallerycleaner.work.ScreenshotDetectorJob
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var appPreferences: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && appPreferences.screenshotNotification) {
            ScreenshotDetectorJob.schedule(context)
        }
    }
}
