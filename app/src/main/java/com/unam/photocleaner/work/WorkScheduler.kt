package com.unam.photocleaner.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    fun schedulePeriodicScan(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<PhotoScanWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.MINUTES)  // 앱 첫 실행 후 30분 뒤부터
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PhotoScanWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,  // 이미 스케줄된 경우 유지
            request,
        )
    }

    fun cancelScan(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PhotoScanWorker.WORK_NAME)
    }
}
