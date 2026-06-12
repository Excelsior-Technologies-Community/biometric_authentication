package com.ext.biometric_auth

import java.io.Serializable

data class BiometricConfig(
    val successColor: Int = 0xFF4CAF50.toInt(),
    val errorColor: Int = 0xFFF44336.toInt(),
    val maxAttempts: Int = 3,
    val lockoutDuration: Long = 30_000L,
    val title: String = "Biometric Authentication",
    val subtitle: String = "Please authenticate to continue",
    val negativeButtonText: String = "Use PIN"
) : Serializable
