package com.namilab.gallerycleaner.domain

import android.net.Uri

interface ScanNotifier {
    fun notifyScanDone(groupCount: Int, savingBytes: Long, sampleUri: Uri? = null, fromWorker: Boolean = false)
    fun notifyScreenshotFavorite(photoId: Long, displayName: String)
}
