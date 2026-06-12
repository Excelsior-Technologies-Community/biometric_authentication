package com.ext.biometric_auth

interface BiometricCallback {
    fun onResult(result: BiometricResult)
}
