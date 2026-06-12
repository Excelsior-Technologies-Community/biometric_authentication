package com.ext.biometric_auth

class LockoutManager(
    private val storageManager: SecureStorageManager,
    private val config: BiometricConfig
) {
    fun getRemainingAttempts(): Int {
        val max = config.maxAttempts
        val currentFailed = storageManager.getFailedAttempts()
        return (max - currentFailed).coerceAtLeast(0)
    }

    fun recordFailedAttempt() {
        val currentFailed = storageManager.getFailedAttempts() + 1
        storageManager.saveFailedAttempts(currentFailed)
        
        if (currentFailed >= config.maxAttempts) {
            storageManager.saveLockoutStartTime(System.currentTimeMillis())
        }
    }

    fun resetAttempts() {
        storageManager.clearLockoutState()
    }

    fun isLockedOut(): Boolean {
        val lockoutStartTime = storageManager.getLockoutStartTime()
        if (lockoutStartTime == 0L) return false

        val elapsed = System.currentTimeMillis() - lockoutStartTime
        if (elapsed >= config.lockoutDuration) {
            resetAttempts()
            return false
        }
        return true
    }

    fun getLockoutTimeRemaining(): Long {
        val lockoutStartTime = storageManager.getLockoutStartTime()
        if (lockoutStartTime == 0L) return 0L

        val elapsed = System.currentTimeMillis() - lockoutStartTime
        val remaining = config.lockoutDuration - elapsed
        return remaining.coerceAtLeast(0L)
    }
}
