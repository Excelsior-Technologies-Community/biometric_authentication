package com.exeintrn.biometric

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.exeintrn.biometric.databinding.ActivityMainBinding
import com.ext.biometric_auth.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var authManager: BiometricAuthManager
    private lateinit var capabilityChecker: BiometricCapabilityChecker

    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Library
        authManager = BiometricAuthManager(this)
        capabilityChecker = BiometricCapabilityChecker(this)

        setupListeners()
        updateUiState()
        checkBiometricCapability()
    }

    private fun checkBiometricCapability() {
        val capability = capabilityChecker.checkCapability()
        val statusText = when (capability) {
            is CapabilityState.Available -> "Available (Strong Biometrics)"
            is CapabilityState.NotEnrolled -> "Not Enrolled (No biometric profiles registered)"
            is CapabilityState.HardwareUnavailable -> "Hardware Unavailable (Sensor is busy/locked)"
            is CapabilityState.HardwareNotPresent -> "No Hardware (Device has no biometric sensors)"
            is CapabilityState.SecurityUpdateRequired -> "Security Update Required"
        }
        binding.tvBiometricStatus.text = "Biometric Status: $statusText"
        logEvent("Biometric status checked: $statusText")
    }

    private fun setupListeners() {
        // Launches Setup Dialog
        binding.btnSetupPin.setOnClickListener {
            logEvent("Launching Setup PIN Dialog...")
            val config = BiometricConfig()
            val handler = PinAuthHandler(this, config, null, onActionCompleted = { mode ->
                if (mode == PinDialogMode.SETUP) {
                    logEvent("PIN setup completed successfully.")
                    updateUiState()
                }
            })
            handler.showPinDialog(PinDialogMode.SETUP)
        }

        binding.btnLogin.setOnClickListener {
            logEvent("Starting authentication request...")
            val callback = object : BiometricCallback {
                override fun onResult(result: BiometricResult) {
                    when (result) {
                        is BiometricResult.AuthenticationSucceeded -> {
                            isLoggedIn = true
                            logEvent("Success: Authenticated via Biometrics.")
                            updateUiState()
                        }
                        is BiometricResult.PinAuthenticationSucceeded -> {
                            isLoggedIn = true
                            logEvent("Success: Authenticated via Fallback PIN.")
                            updateUiState()
                        }
                        is BiometricResult.AuthenticationFailed -> {
                            logEvent("Error: ${result.reason}")
                        }
                        is BiometricResult.BiometricNotAvailable -> {
                            logEvent("Error: Biometrics unavailable.")
                        }
                        is BiometricResult.BiometricNotEnrolled -> {
                            logEvent("Error: No biometrics enrolled.")
                        }
                        is BiometricResult.AuthenticationCancelled -> {
                            logEvent("Cancelled: Authentication prompt dismissed.")
                        }
                        is BiometricResult.LockoutActive -> {
                            logEvent("Lockout: Too many failures. Input locked.")
                        }
                    }
                }
            }
            authManager.authenticate(this, callback)
        }

        binding.btnResetPin.setOnClickListener {
            logEvent("Launching Reset PIN Dialog...")
            val config = BiometricConfig()
            val handler = PinAuthHandler(this, config, null, onActionCompleted = { mode ->
                if (mode == PinDialogMode.SETUP) {
                    logEvent("PIN reset completed successfully.")
                    updateUiState()
                }
            })
            handler.showPinDialog(PinDialogMode.RESET)
        }

        binding.btnClearPin.setOnClickListener {
            logEvent("Launching Remove PIN Dialog...")
            val config = BiometricConfig()
            val handler = PinAuthHandler(this, config, null, onActionCompleted = { mode ->
                if (mode == PinDialogMode.REMOVE) {
                    logEvent("PIN removed successfully.")
                    updateUiState()
                }
            })
            handler.showPinDialog(PinDialogMode.REMOVE)
        }

        binding.btnLogout.setOnClickListener {
            isLoggedIn = false
            logEvent("User logged out.")
            updateUiState()
        }
    }

    private fun updateUiState() {
        val pinConfigured = authManager.isPinConfigured()

        if (isLoggedIn) {
            binding.cardOnboarding.visibility = View.GONE
            binding.cardLogin.visibility = View.GONE
            binding.cardHome.visibility = View.VISIBLE
        } else {
            binding.cardHome.visibility = View.GONE
            if (pinConfigured) {
                binding.cardOnboarding.visibility = View.GONE
                binding.cardLogin.visibility = View.VISIBLE
            } else {
                binding.cardOnboarding.visibility = View.VISIBLE
                binding.cardLogin.visibility = View.GONE
            }
        }
    }

    private fun logEvent(message: String) {
        val current = binding.tvConsoleLogs.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        binding.tvConsoleLogs.text = "[$timestamp] $message\n$current"
    }
}