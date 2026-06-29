package com.unam.gallerycleaner.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unam.gallerycleaner.data.local.MediaStoreDataSource
import com.unam.gallerycleaner.domain.ScanNotifier
import com.unam.gallerycleaner.domain.usecase.GroupPhotosUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GalleryScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaStore: MediaStoreDataSource,
    private val groupPhotos: GroupPhotosUseCase,
    private val scanNotifier: ScanNotifier,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastScanned = prefs.getLong(KEY_LAST_SCANNED, defaultStartTime())

        val recentPhotos = mediaStore.getPhotosSince(lastScanned)
        if (recentPhotos.isEmpty()) {
            updateLastScanned(prefs)
            return Result.success()
        }

        val groups = groupPhotos.execute(recentPhotos)
        if (groups.isNotEmpty()) {
            scanNotifier.notifyScanDone(
                groupCount = groups.size,
                savingBytes = groups.sumOf { it.potentialSavingBytes },
            )
        }

        updateLastScanned(prefs)
        return Result.success()
    }

    private fun defaultStartTime() =
        System.currentTimeMillis() - 24 * 60 * 60 * 1000L

    private fun updateLastScanned(prefs: android.content.SharedPreferences) {
        prefs.edit().putLong(KEY_LAST_SCANNED, System.currentTimeMillis()).apply()
    }

    companion object {
        const val WORK_NAME = "photo_scan_periodic"
        const val PREFS_NAME = "photo_cleaner_prefs"
        const val KEY_LAST_SCANNED = "last_scanned_at"
    }
}
