package com.ext.biometric_auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorageManager(private val context: Context) {
    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                AuthConstants.PREFS_SECURE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(AuthConstants.PREFS_FALLBACK_NAME, Context.MODE_PRIVATE)
        }
    }

    private val lockoutPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(AuthConstants.PREFS_LOCKOUT_NAME, Context.MODE_PRIVATE)
    }

    fun savePinHashAndSalt(hash: String, salt: String) {
        securePrefs.edit()
            .putString(AuthConstants.KEY_PIN_HASH, hash)
            .putString(AuthConstants.KEY_PIN_SALT, salt)
            .apply()
    }

    fun getPinHash(): String? {
        return securePrefs.getString(AuthConstants.KEY_PIN_HASH, null)
    }

    fun getPinSalt(): String? {
        return securePrefs.getString(AuthConstants.KEY_PIN_SALT, null)
    }

    fun clearPin() {
        securePrefs.edit()
            .remove(AuthConstants.KEY_PIN_HASH)
            .remove(AuthConstants.KEY_PIN_SALT)
            .apply()
    }

    fun saveFailedAttempts(attempts: Int) {
        lockoutPrefs.edit().putInt(AuthConstants.KEY_FAILED_ATTEMPTS, attempts).apply()
    }

    fun getFailedAttempts(): Int {
        return lockoutPrefs.getInt(AuthConstants.KEY_FAILED_ATTEMPTS, 0)
    }

    fun saveLockoutStartTime(timestamp: Long) {
        lockoutPrefs.edit().putLong(AuthConstants.KEY_LOCKOUT_START_TIME, timestamp).apply()
    }

    fun getLockoutStartTime(): Long {
        return lockoutPrefs.getLong(AuthConstants.KEY_LOCKOUT_START_TIME, 0L)
    }

    fun clearLockoutState() {
        lockoutPrefs.edit()
            .remove(AuthConstants.KEY_FAILED_ATTEMPTS)
            .remove(AuthConstants.KEY_LOCKOUT_START_TIME)
            .apply()
    }
}
