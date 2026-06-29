package com.unam.gallerycleaner.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun schedulePeriodicScan() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val request = PeriodicWorkRequestBuilder<GalleryScanWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            GalleryScanWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelScan() {
        WorkManager.getInstance(context).cancelUniqueWork(GalleryScanWorker.WORK_NAME)
    }

    fun triggerNow() {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<GalleryScanWorker>().build()
        )
    }
}
