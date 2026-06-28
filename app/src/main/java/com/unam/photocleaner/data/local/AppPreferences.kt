package com.unam.photocleaner.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs = context.getSharedPreferences("photo_cleaner_prefs", Context.MODE_PRIVATE)

    var periodicScanNotification: Boolean
        get() = prefs.getBoolean(KEY_PERIODIC_NOTIFICATION, true)
        set(v) = prefs.edit { putBoolean(KEY_PERIODIC_NOTIFICATION, v) }

    var screenshotNotification: Boolean
        get() = prefs.getBoolean(KEY_SCREENSHOT_NOTIFICATION, true)
        set(v) = prefs.edit { putBoolean(KEY_SCREENSHOT_NOTIFICATION, v) }

    companion object {
        const val KEY_PERIODIC_NOTIFICATION = "periodic_notification"
        const val KEY_SCREENSHOT_NOTIFICATION = "screenshot_notification"
    }
}
