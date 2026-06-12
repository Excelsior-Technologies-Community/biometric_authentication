package com.ext.biometric_auth

import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

class PinSecurityManager {
    
    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(AuthConstants.SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val combined = pin.toByteArray(Charsets.UTF_8) + salt
        return digest.digest(combined)
    }

    fun verifyPin(pin: String, storedHashBase64: String, storedSaltBase64: String): Boolean {
        return try {
            val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
            val hash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
            val computedHash = hashPin(pin, salt)
            computedHash.contentEquals(hash)
        } catch (e: Exception) {
            false
        }
    }
}
