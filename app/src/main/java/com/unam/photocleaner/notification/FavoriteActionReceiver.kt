package com.unam.photocleaner.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unam.photocleaner.data.local.db.FavoritePhotoDao
import com.unam.photocleaner.data.local.db.FavoritePhotoEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FavoriteActionReceiver : BroadcastReceiver() {

    @Inject lateinit var favoriteDao: FavoritePhotoDao
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val photoId = intent.getLongExtra(EXTRA_PHOTO_ID, -1L)
        if (photoId < 0) return
        val displayName = intent.getStringExtra(EXTRA_DISPLAY_NAME) ?: ""
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                favoriteDao.insert(FavoritePhotoEntity(photoId))
                notificationHelper.showScreenshotMarkedConfirm(displayName)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_MARK_FAVORITE = "com.unam.photocleaner.ACTION_MARK_FAVORITE"
        const val EXTRA_PHOTO_ID = "extra_photo_id"
        const val EXTRA_DISPLAY_NAME = "extra_display_name"
    }
}
