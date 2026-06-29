package com.unam.gallerycleaner.work

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.provider.MediaStore
import com.unam.gallerycleaner.data.local.AppPreferences
import com.unam.gallerycleaner.domain.ScanNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScreenshotDetectorJob : JobService() {

    @Inject lateinit var scanNotifier: ScanNotifier
    @Inject lateinit var appPreferences: AppPreferences

    override fun onStartJob(params: JobParameters): Boolean {
        if (appPreferences.screenshotNotification) {
            findLatestScreenshot()?.let { (photoId, displayName) ->
                scanNotifier.notifyScreenshotFavorite(photoId, displayName)
            }
        }
        schedule(this)
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    private fun findLatestScreenshot(): Pair<Long, String>? {
        val sinceSeconds = System.currentTimeMillis() / 1000 - 60
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
        )
        val selection = "${MediaStore.Images.Media.DATE_ADDED} > ? AND " +
            "(${MediaStore.Images.Media.RELATIVE_PATH} LIKE ? OR " +
            "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?)"
        val args = arrayOf(sinceSeconds.toString(), "%creenshot%", "Screenshot_%")

        contentResolver.query(
            collection, projection, selection, args,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: ""
                return id to name
            }
        }
        return null
    }

    companion object {
        private const val JOB_ID = 2001

        fun schedule(context: Context) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, ScreenshotDetectorJob::class.java))
                .addTriggerContentUri(
                    JobInfo.TriggerContentUri(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS,
                    )
                )
                .setTriggerContentMaxDelay(2_000L)
                .setTriggerContentUpdateDelay(500L)
                .build()
            (context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(job)
        }

        fun cancel(context: Context) {
            (context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler).cancel(JOB_ID)
        }
    }
}
