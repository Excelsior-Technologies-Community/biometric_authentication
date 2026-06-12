package com.ext.biometric_auth

object AuthConstants {
    const val SALT_LENGTH = 16
    const val DEFAULT_MAX_ATTEMPTS = 3
    const val DEFAULT_LOCKOUT_DURATION = 30000L // 30 seconds
    
    // Shared Preferences Keys
    const val PREFS_SECURE_NAME = "secure_biometric_prefs"
    const val PREFS_FALLBACK_NAME = "fallback_biometric_prefs"
    const val PREFS_LOCKOUT_NAME = "biometric_lockout_prefs"
    
    const val KEY_PIN_HASH = "pin_hash"
    const val KEY_PIN_SALT = "pin_salt"
    const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    const val KEY_LOCKOUT_START_TIME = "lockout_start_time"
}
