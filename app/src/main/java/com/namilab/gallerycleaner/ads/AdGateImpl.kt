package com.namilab.gallerycleaner.ads

import android.content.Context
import com.namilab.gallerycleaner.domain.AdGate
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdGateImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AdGate {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun isUnlocked(): Boolean = isPremium() || remainingMs() > 0

    override fun unlock() {
        prefs.edit()
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + UNLOCK_DURATION_MS)
            .apply()
    }

    override fun remainingMs(): Long {
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        return (expiresAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    override fun isPremium(): Boolean = prefs.getBoolean(KEY_PREMIUM, false)

    override fun setPremium(value: Boolean) {
        prefs.edit().putBoolean(KEY_PREMIUM, value).apply()
    }

    companion object {
        private const val PREFS_NAME = "ad_gate_prefs"
        private const val KEY_EXPIRES_AT = "ad_free_expires_at"
        private const val KEY_PREMIUM = "is_premium"
        private const val UNLOCK_DURATION_MS = 60 * 60 * 1000L // 1시간
    }
}
