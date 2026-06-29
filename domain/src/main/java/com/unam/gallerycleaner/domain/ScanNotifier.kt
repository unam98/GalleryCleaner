package com.unam.gallerycleaner.domain

interface ScanNotifier {
    fun notifyScanDone(groupCount: Int, savingBytes: Long)
    fun notifyScreenshotFavorite(photoId: Long, displayName: String)
}
