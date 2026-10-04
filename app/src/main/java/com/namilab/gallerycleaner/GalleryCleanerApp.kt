package com.namilab.gallerycleaner

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder
import com.namilab.gallerycleaner.data.local.AppPreferences
import com.namilab.gallerycleaner.notification.NotificationHelper
import com.namilab.gallerycleaner.work.ScreenshotDetectorJob
import com.namilab.gallerycleaner.work.WorkScheduler
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GalleryCleanerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate() {
        super.onCreate()
        // 동영상 썸네일 지원 (VideoFrameDecoder)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components { add(VideoFrameDecoder.Factory()) }
                .build()
        )
        MobileAds.initialize(this)
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
