package com.unam.photocleaner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.unam.photocleaner.data.local.AppPreferences
import com.unam.photocleaner.notification.NotificationHelper
import com.unam.photocleaner.work.ScreenshotDetectorJob
import com.unam.photocleaner.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PhotoCleanerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        if (appPreferences.periodicScanNotification) {
            workScheduler.schedulePeriodicScan()
        }
        if (appPreferences.screenshotNotification) {
            ScreenshotDetectorJob.schedule(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
