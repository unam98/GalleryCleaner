package com.namilab.gallerycleaner.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.namilab.gallerycleaner.data.local.MediaStoreDataSource
import com.namilab.gallerycleaner.domain.GroupResultsCache
import com.namilab.gallerycleaner.domain.ScanNotifier
import com.namilab.gallerycleaner.domain.usecase.GroupPhotosUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GalleryScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val mediaStore: MediaStoreDataSource,
    private val groupPhotos: GroupPhotosUseCase,
    private val scanNotifier: ScanNotifier,
    private val groupResultsCache: GroupResultsCache,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val forceFull = inputData.getBoolean(KEY_FORCE_FULL, false)
        val lastScanned = if (forceFull) 0L else prefs.getLong(KEY_LAST_SCANNED, defaultStartTime())

        val recentPhotos = mediaStore.getPhotosSince(lastScanned)
        if (recentPhotos.isEmpty()) {
            updateLastScanned(prefs)
            return Result.success()
        }

        // 백그라운드 Worker: TFLite 건너뛰고 dHash 기반 정확 중복·버스트만 검출
        val groups = groupPhotos.execute(recentPhotos, skipSimilar = true)
        if (groups.isNotEmpty()) {
            val totalSaving = groups.sumOf { it.potentialSavingBytes }
            // 앱 진입 시 바로 Done 상태로 복원되도록 in-memory 캐시에 저장
            groupResultsCache.save(groups, totalSaving)
            val topGroup = groups.maxByOrNull { it.potentialSavingBytes }
            // 프로세스 사망 후 알림 탭 대비: SharedPreferences에도 top 그룹 photo ID 영속 저장
            if (topGroup != null) {
                prefs.edit()
                    .putString(KEY_PENDING_PHOTO_IDS, topGroup.photos.joinToString(",") { it.id.toString() })
                    .putLong(KEY_PENDING_BEST_ID, topGroup.bestPhotoId)
                    .putLong(KEY_PENDING_SAVING, topGroup.potentialSavingBytes)
                    .apply()
            }
            val sampleUri = topGroup?.photos?.find { it.id == topGroup.bestPhotoId }?.uri
                ?: groups.firstOrNull()?.photos?.firstOrNull()?.uri
            scanNotifier.notifyScanDone(
                groupCount = groups.size,
                savingBytes = totalSaving,
                sampleUri = sampleUri,
                fromWorker = true,
            )
        } else if (forceFull) {
            // 디버그 강제 실행: 중복 없어도 알림 동작 확인용 mock 발송
            scanNotifier.notifyScanDone(groupCount = 3, savingBytes = 52_428_800L)
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
        const val KEY_FORCE_FULL = "force_full"
        const val KEY_PENDING_PHOTO_IDS = "pending_group_photo_ids"
        const val KEY_PENDING_BEST_ID = "pending_group_best_id"
        const val KEY_PENDING_SAVING = "pending_group_saving_bytes"
    }
}
