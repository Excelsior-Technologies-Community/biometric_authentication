package com.ext.biometric_auth

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthManager(
    private val context: Context,
    private val config: BiometricConfig = BiometricConfig()
) {
    private val capabilityChecker = BiometricCapabilityChecker(context)
    private val storageManager = SecureStorageManager(context)
    private val lockoutManager = LockoutManager(storageManager, config)
    private val pinSecurityManager = PinSecurityManager()

    fun authenticate(
        activity: FragmentActivity,
        callback: BiometricCallback
    ) {
        // 1. Check lockout status first
        if (lockoutManager.isLockedOut()) {
            callback.onResult(BiometricResult.LockoutActive)
            return
        }

        // 2. Check biometric hardware and enrollment capability
        val capability = capabilityChecker.checkCapability()
        if (capability != CapabilityState.Available) {
            if (isPinConfigured()) {
                showPinFallback(activity, callback)
            } else {
                val result = when (capability) {
                    CapabilityState.NotEnrolled -> BiometricResult.BiometricNotEnrolled
                    else -> BiometricResult.BiometricNotAvailable
                }
                callback.onResult(result)
            }
            return
        }

        // 3. Launch Android BiometricPrompt
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> {
                            callback.onResult(BiometricResult.AuthenticationCancelled)
                        }
                        BiometricPrompt.ERROR_LOCKOUT,
                        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                            // Biometric lockout occurred. Switch automatically to PIN fallback.
                            if (isPinConfigured()) {
                                showPinFallback(activity, callback)
                            } else {
                                callback.onResult(BiometricResult.LockoutActive)
                            }
                        }
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                            // User chose to use PIN fallback
                            if (isPinConfigured()) {
                                showPinFallback(activity, callback)
                            } else {
                                callback.onResult(BiometricResult.AuthenticationCancelled)
                            }
                        }
                        else -> {
                            if (isPinConfigured()) {
                                showPinFallback(activity, callback)
                            } else {
                                callback.onResult(BiometricResult.AuthenticationFailed(errString.toString()))
                            }
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    lockoutManager.resetAttempts()
                    callback.onResult(BiometricResult.AuthenticationSucceeded)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Biometric scanning failed once, handled by framework.
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(config.title)
            .setSubtitle(config.subtitle)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText(config.negativeButtonText)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun showPinFallback(activity: FragmentActivity, callback: BiometricCallback) {
        val handler = PinAuthHandler(activity, config, callback)
        handler.showPinDialog(PinDialogMode.VERIFY)
    }

    fun setupPin(pin: String) {
        val salt = pinSecurityManager.generateSalt()
        val hash = pinSecurityManager.hashPin(pin, salt)

        val saltStr = Base64.encodeToString(salt, Base64.NO_WRAP)
        val hashStr = Base64.encodeToString(hash, Base64.NO_WRAP)

        storageManager.savePinHashAndSalt(hashStr, saltStr)
        lockoutManager.resetAttempts()
    }

    fun verifyPin(pin: String): Boolean {
        if (lockoutManager.isLockedOut()) {
            return false
        }
        val saltStr = storageManager.getPinSalt()
        val hashStr = storageManager.getPinHash()
        if (saltStr == null || hashStr == null) {
            return false
        }

        val success = pinSecurityManager.verifyPin(pin, hashStr, saltStr)
        if (success) {
            lockoutManager.resetAttempts()
        } else {
            lockoutManager.recordFailedAttempt()
        }
        return success
    }

    fun resetPin(oldPin: String, newPin: String) {
        val saltStr = storageManager.getPinSalt()
        val hashStr = storageManager.getPinHash()
        if (saltStr == null || hashStr == null) {
            throw IllegalStateException("PIN is not configured")
        }

        val success = pinSecurityManager.verifyPin(oldPin, hashStr, saltStr)
        if (!success) {
            throw IllegalArgumentException("Incorrect old PIN")
        }

        setupPin(newPin)
    }

    fun removePin() {
        storageManager.clearPin()
        lockoutManager.resetAttempts()
    }

    fun isPinConfigured(): Boolean {
        return storageManager.getPinHash() != null && storageManager.getPinSalt() != null
    }
}
