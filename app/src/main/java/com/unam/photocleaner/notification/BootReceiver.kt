package com.unam.photocleaner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unam.photocleaner.data.local.AppPreferences
import com.unam.photocleaner.work.ScreenshotDetectorJob
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
