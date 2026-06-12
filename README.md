# Secure Biometric Authentication Library with PIN Fallback (`biometric_auth`)

A production-ready, security-hardened Android library module providing secure biometric authentication (Fingerprint, Face Unlock, and Iris Recognition) with a custom PIN-based fallback dialog. 

This library follows clean architecture principles, SOLID design, Kotlin coroutines, and lifecycle-safe implementation, and implements cryptographic security guidelines for secure key storage and brute-force protection.

---

## Architecture and Cryptographic Security

### 1. Plaintext PIN Protection (Salt + Hash)
The library strictly adheres to the principle of never storing plaintext PINs. Instead:
- **Salt Generation:** Generates a cryptographically strong 16-byte random salt using `PinSecurityManager`.
- **Combination & Hashing:** Concatenates the user's PIN with the salt and hashes the result using `SHA-256`.
- **Secure Storage:** Base64-encodes the hash and the salt, storing them using AndroidX `EncryptedSharedPreferences` (which leverages Android Keystore System for hardware-backed encryption).
- **Graceful Fallback:** If `EncryptedSharedPreferences` fails due to device-specific Keystore issues, it falls back to standard `SharedPreferences` to ensure app reliability.

### 2. Lockout Protection (Brute Force Prevention)
- Tracks failed attempts persistently using private `SharedPreferences` so that lockouts survive application restarts.
- Configurable thresholds (default: `maxAttempts = 3`, `lockoutDuration = 30 seconds`).
- When lockout is active, the custom PIN input dialog automatically disables all numeric keypad buttons and initiates a countdown timer.

---

## Installation

### 1. Register the Module in settings.gradle.kts
Add `:biometric_auth` to your `settings.gradle.kts` file:
```kotlin
include(":app")
include(":biometric_auth")
```

### 2. Add Dependencies
Include the library module in your application module's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":biometric_auth"))
}
```

Ensure your version catalogs (`libs.versions.toml`) declare:
```toml
[versions]
biometric = "1.2.0-alpha05"
securityCrypto = "1.1.0-alpha06"

[libraries]
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }
androidx-security-crypto = { group = "androidx.security", name = "security-crypto", version.ref = "securityCrypto" }
```

---

## API Documentation

### `BiometricConfig`
Customizable properties for UI and behavior overrides.

| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `successColor` | `Int` | `0xFF4CAF50` (Green) | Color of success visual feedback. |
| `errorColor` | `Int` | `0xFFF44336` (Red) | Color of error animations/shaking dots. |
| `maxAttempts` | `Int` | `3` | Maximum wrong PIN attempts before lockout. |
| `lockoutDuration` | `Long` | `30_000L` (30s) | Lockout duration in milliseconds. |
| `title` | `String` | `"Biometric Authentication"`| Custom dialog Title. |
| `subtitle` | `String` | `"Please authenticate"` | Custom dialog Subtitle. |
| `negativeButtonText`| `String` | `"Use PIN"` | Label on the biometric fallback action. |

---

### `BiometricResult`
Sealed class returned inside the callback:
- `AuthenticationSucceeded` — Authenticated successfully via Android biometric prompt.
- `PinAuthenticationSucceeded` — Authenticated successfully via fallback PIN dialog.
- `AuthenticationFailed(val reason: String)` — Authentication failed or incorrect PIN.
- `BiometricNotAvailable` — Device lacks biometric hardware or capability is disabled.
- `BiometricNotEnrolled` — Device supports biometrics but no profiles are enrolled.
- `AuthenticationCancelled` — User cancelled the prompt.
- `LockoutActive` — Lockout is currently active on the device.

---

## Integration and Usage

### 1. Initialize the Manager
Initialize `BiometricAuthManager` in your Activity:
```kotlin
import com.ext.biometric_auth.BiometricAuthManager
import com.ext.biometric_auth.BiometricConfig

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: BiometricAuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Use default configuration
        authManager = BiometricAuthManager(this)
    }
}
```

### 2. Onboarding: PIN Setup and Registration
To register a new security PIN (launches a self-contained double-entry confirmation dialog):
```kotlin
val handler = PinAuthHandler(this, BiometricConfig(), null, onActionCompleted = { mode ->
    if (mode == PinDialogMode.SETUP) {
        // PIN saved successfully
    }
})
handler.showPinDialog(PinDialogMode.SETUP)
```

### 3. Verification
Verify credentials using the fallback PIN Dialog:
```kotlin
val callback = object : BiometricCallback {
    override fun onResult(result: BiometricResult) {
        when (result) {
            is BiometricResult.PinAuthenticationSucceeded -> { /* Authenticated */ }
            // ...
        }
    }
}

val handler = PinAuthHandler(this, BiometricConfig(), callback)
handler.showPinDialog(PinDialogMode.VERIFY)
```

### 4. Reset PIN
Change security PIN credentials (verifies the current PIN inside the dialog, then prompts for the new one):
```kotlin
val handler = PinAuthHandler(this, BiometricConfig(), null, onActionCompleted = { mode ->
    if (mode == PinDialogMode.SETUP) {
        // PIN reset successfully
    }
})
handler.showPinDialog(PinDialogMode.RESET)
```

### 5. Remove PIN
Delete PIN credentials (verifies the identity via current PIN before deletion):
```kotlin
val handler = PinAuthHandler(this, BiometricConfig(), null, onActionCompleted = { mode ->
    if (mode == PinDialogMode.REMOVE) {
        // PIN cleared successfully
    }
})
handler.showPinDialog(PinDialogMode.REMOVE)
```

### 6. Trigger Core Authentication Flow
Call `authenticate` to run the secure flow. If biometric is unavailable or the user chooses the fallback, the PIN Dialog is automatically shown:
```kotlin
val callback = object : BiometricCallback {
    override fun onResult(result: BiometricResult) {
        when (result) {
            is BiometricResult.AuthenticationSucceeded -> {
                // Success: User logged in via Face/Fingerprint
            }
            is BiometricResult.PinAuthenticationSucceeded -> {
                // Success: User logged in via Fallback PIN
            }
            is BiometricResult.LockoutActive -> {
                // Lockout in progress
            }
            is BiometricResult.AuthenticationFailed -> {
                // Display error reason
            }
            is BiometricResult.AuthenticationCancelled -> {
                // User closed the authentication prompt
            }
            else -> {
                // Handle other states
            }
        }
    }
}

authManager.authenticate(this, callback)
```
