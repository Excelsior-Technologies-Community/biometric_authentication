package com.exeintrn.biometric

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ext.biometric_auth.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    private lateinit var authManager: BiometricAuthManager
    private lateinit var capabilityChecker: BiometricCapabilityChecker

    private lateinit var tvBiometricStatus: TextView
    private lateinit var tvConsoleLogs: TextView

    private lateinit var cardOnboarding: MaterialCardView
    private lateinit var cardLogin: MaterialCardView
    private lateinit var cardHome: MaterialCardView

    private lateinit var btnStartSetup: MaterialButton
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnResetPin: MaterialButton
    private lateinit var btnClearPin: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var isLoggedIn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Library
        authManager = BiometricAuthManager(this)
        capabilityChecker = BiometricCapabilityChecker(this)

        // Bind Views
        tvBiometricStatus = findViewById(R.id.tv_biometric_status)
        tvConsoleLogs = findViewById(R.id.tv_console_logs)

        cardOnboarding = findViewById(R.id.card_onboarding)
        cardLogin = findViewById(R.id.card_login)
        cardHome = findViewById(R.id.card_home)

        btnStartSetup = findViewById(R.id.btn_setup_pin)
        btnLogin = findViewById(R.id.btn_login)
        btnResetPin = findViewById(R.id.btn_reset_pin)
        btnClearPin = findViewById(R.id.btn_clear_pin)
        btnLogout = findViewById(R.id.btn_logout)

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
        tvBiometricStatus.text = "Biometric Status: $statusText"
        logEvent("Biometric status checked: $statusText")
    }

    private fun setupListeners() {
        // Launches Setup Dialog
        btnStartSetup.setOnClickListener {
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

        btnLogin.setOnClickListener {
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

        btnResetPin.setOnClickListener {
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

        btnClearPin.setOnClickListener {
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

        btnLogout.setOnClickListener {
            isLoggedIn = false
            logEvent("User logged out.")
            updateUiState()
        }
    }

    private fun updateUiState() {
        val pinConfigured = authManager.isPinConfigured()

        if (isLoggedIn) {
            cardOnboarding.visibility = View.GONE
            cardLogin.visibility = View.GONE
            cardHome.visibility = View.VISIBLE
        } else {
            cardHome.visibility = View.GONE
            if (pinConfigured) {
                cardOnboarding.visibility = View.GONE
                cardLogin.visibility = View.VISIBLE
            } else {
                cardOnboarding.visibility = View.VISIBLE
                cardLogin.visibility = View.GONE
            }
        }
    }

    private fun logEvent(message: String) {
        val current = tvConsoleLogs.text.toString()
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        tvConsoleLogs.text = "[$timestamp] $message\n$current"
    }
}