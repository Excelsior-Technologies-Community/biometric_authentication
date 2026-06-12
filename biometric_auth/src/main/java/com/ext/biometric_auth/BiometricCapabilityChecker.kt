package com.ext.biometric_auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG

sealed class CapabilityState {
    data object Available : CapabilityState()
    data object NotEnrolled : CapabilityState()
    data object HardwareNotPresent : CapabilityState()
    data object HardwareUnavailable : CapabilityState()
    data object SecurityUpdateRequired : CapabilityState()
}

class BiometricCapabilityChecker(private val context: Context) {
    fun checkCapability(): CapabilityState {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> CapabilityState.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> CapabilityState.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> CapabilityState.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> CapabilityState.HardwareNotPresent
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> CapabilityState.SecurityUpdateRequired
            else -> CapabilityState.HardwareNotPresent
        }
    }
}
