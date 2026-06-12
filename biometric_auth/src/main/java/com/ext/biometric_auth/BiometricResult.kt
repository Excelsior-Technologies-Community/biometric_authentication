package com.ext.biometric_auth

sealed class BiometricResult {
    data object AuthenticationSucceeded : BiometricResult()
    data object PinAuthenticationSucceeded : BiometricResult()
    data class AuthenticationFailed(val reason: String) : BiometricResult()
    data object BiometricNotAvailable : BiometricResult()
    data object BiometricNotEnrolled : BiometricResult()
    data object AuthenticationCancelled : BiometricResult()
    data object LockoutActive : BiometricResult()
}
