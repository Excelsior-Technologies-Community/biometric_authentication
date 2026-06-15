package com.ext.biometric_auth

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.ext.biometric_auth.databinding.DialogPinAuthBinding
import com.google.android.material.button.MaterialButton
import java.io.Serializable

enum class PinDialogMode : Serializable {
    SETUP,
    CONFIRM_SETUP,
    VERIFY,
    RESET,
    REMOVE
}

class PinAuthHandler(
    private val activity: FragmentActivity,
    private val config: BiometricConfig,
    private val callback: BiometricCallback? = null,
    private val onActionCompleted: ((PinDialogMode) -> Unit)? = null
) {
    fun showPinDialog(mode: PinDialogMode) {
        val dialog = PinAuthDialogFragment.newInstance(config, mode)
        dialog.setCallback(object : PinAuthDialogFragment.Callback {
            override fun onPinSuccess(completedMode: PinDialogMode) {
                if (completedMode == PinDialogMode.VERIFY) {
                    callback?.onResult(BiometricResult.PinAuthenticationSucceeded)
                } else {
                    onActionCompleted?.invoke(completedMode)
                }
            }

            override fun onCancelled() {
                callback?.onResult(BiometricResult.AuthenticationCancelled)
            }
        })
        dialog.show(activity.supportFragmentManager, "PinAuthDialogFragment")
    }
}

class PinAuthDialogFragment : DialogFragment() {

    interface Callback {
        fun onPinSuccess(mode: PinDialogMode)
        fun onCancelled()
    }

    private var callback: Callback? = null
    private lateinit var config: BiometricConfig
    private lateinit var currentMode: PinDialogMode
    private var tempPinForSetup: String? = null
    
    private val enteredPin = StringBuilder()
    
    private var _binding: DialogPinAuthBinding? = null
    private val binding get() = _binding!!

    private val keypadButtons = ArrayList<MaterialButton>()
    private var countDownTimer: CountDownTimer? = null
    
    private lateinit var authManager: BiometricAuthManager
    private lateinit var lockoutManager: LockoutManager

    companion object {
        fun newInstance(config: BiometricConfig, mode: PinDialogMode): PinAuthDialogFragment {
            return PinAuthDialogFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("config", config)
                    putSerializable("mode", mode)
                }
            }
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        config = arguments?.getSerializable("config") as? BiometricConfig ?: BiometricConfig()
        currentMode = arguments?.getSerializable("mode") as? PinDialogMode ?: PinDialogMode.VERIFY
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogPinAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authManager = BiometricAuthManager(requireContext(), config)
        
        val storageManager = SecureStorageManager(requireContext())
        lockoutManager = LockoutManager(storageManager, config)

        val buttons = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9
        )
        
        for (btn in buttons) {
            keypadButtons.add(btn)
            btn.setOnClickListener {
                handleNumberClick(btn.text.toString())
            }
        }

        binding.btnDelete.setOnClickListener {
            handleDeleteClick()
        }

        binding.btnCancel.setOnClickListener {
            callback?.onCancelled()
            dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            submitPin()
        }

        updateLabelsForMode()
        updatePinDots()
        updateConfirmButtonState()
        checkLockoutState()
    }

    private fun updateLabelsForMode() {
        when (currentMode) {
            PinDialogMode.SETUP -> {
                binding.dialogTitle.text = "Create PIN"
                binding.dialogSubtitle.text = "Enter a new 4-8 digit security PIN"
            }
            PinDialogMode.CONFIRM_SETUP -> {
                binding.dialogTitle.text = "Confirm PIN"
                binding.dialogSubtitle.text = "Re-enter your PIN to verify"
            }
            PinDialogMode.VERIFY -> {
                binding.dialogTitle.text = config.title
                binding.dialogSubtitle.text = config.subtitle
            }
            PinDialogMode.RESET -> {
                binding.dialogTitle.text = "Reset PIN"
                binding.dialogSubtitle.text = "Enter your current PIN to authenticate"
            }
            PinDialogMode.REMOVE -> {
                binding.dialogTitle.text = "Remove PIN"
                binding.dialogSubtitle.text = "Enter your current PIN to remove lock"
            }
        }
    }

    private fun handleNumberClick(num: String) {
        if (enteredPin.length < 8) {
            enteredPin.append(num)
            updatePinDots()
            updateConfirmButtonState()
        }
    }

    private fun handleDeleteClick() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updatePinDots()
            updateConfirmButtonState()
        }
    }

    private fun updatePinDots() {
        binding.dotContainer.removeAllViews()
        val totalDots = maxOf(4, enteredPin.length)
        val primaryColor = getThemeColor(requireContext(), android.R.attr.colorPrimary, Color.BLUE)

        for (i in 0 until totalDots) {
            val dotView = View(context).apply {
                val size = resources.getDimensionPixelSize(R.dimen.pin_dot_size)
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(
                        resources.getDimensionPixelSize(R.dimen.pin_dot_margin),
                        0,
                        resources.getDimensionPixelSize(R.dimen.pin_dot_margin),
                        0
                    )
                }
                
                if (i < enteredPin.length) {
                    setBackgroundResource(R.drawable.pin_dot_filled)
                    backgroundTintList = ColorStateList.valueOf(primaryColor)
                } else {
                    setBackgroundResource(R.drawable.pin_dot_empty)
                }
            }
            binding.dotContainer.addView(dotView)
        }
    }

    private fun getThemeColor(context: Context, attr: Int, defaultColor: Int): Int {
        val typedValue = TypedValue()
        val theme = context.theme
        return if (theme.resolveAttribute(attr, typedValue, true)) {
            typedValue.data
        } else {
            defaultColor
        }
    }

    private fun updateConfirmButtonState() {
        binding.btnConfirm.isEnabled = enteredPin.length in 4..8
    }

    private fun checkLockoutState() {
        val needsLockoutCheck = currentMode == PinDialogMode.VERIFY || 
                                currentMode == PinDialogMode.RESET || 
                                currentMode == PinDialogMode.REMOVE
                                
        if (needsLockoutCheck && lockoutManager.isLockedOut()) {
            val remaining = lockoutManager.getLockoutTimeRemaining()
            startLockoutTimer(remaining)
        } else {
            enableKeypad(true)
            binding.lockoutTimer.visibility = View.GONE
        }
    }

    private fun startLockoutTimer(millis: Long) {
        enableKeypad(false)
        binding.lockoutTimer.visibility = View.VISIBLE
        
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(millis, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = (millisUntilFinished / 1000).coerceAtLeast(1)
                binding.lockoutTimer.text = getString(R.string.lockout_timer_text, seconds)
            }

            override fun onFinish() {
                binding.lockoutTimer.visibility = View.GONE
                enableKeypad(true)
                lockoutManager.resetAttempts()
                updatePinDots()
            }
        }.start()
    }

    private fun enableKeypad(enabled: Boolean) {
        for (btn in keypadButtons) {
            btn.isEnabled = enabled
        }
        binding.btnDelete.isEnabled = enabled
        binding.btnCancel.isEnabled = true
        if (!enabled) {
            binding.btnConfirm.isEnabled = false
        } else {
            updateConfirmButtonState()
        }
    }

    private fun submitPin() {
        val pin = enteredPin.toString()
        
        when (currentMode) {
            PinDialogMode.SETUP -> {
                tempPinForSetup = pin
                currentMode = PinDialogMode.CONFIRM_SETUP
                enteredPin.clear()
                updateLabelsForMode()
                updatePinDots()
                updateConfirmButtonState()
            }
            PinDialogMode.CONFIRM_SETUP -> {
                if (pin == tempPinForSetup) {
                    authManager.setupPin(pin)
                    callback?.onPinSuccess(PinDialogMode.SETUP)
                    dismiss()
                } else {
                    showErrorFeedback("PINs do not match. Try again.")
                    currentMode = PinDialogMode.SETUP
                    tempPinForSetup = null
                    updateLabelsForMode()
                }
            }
            PinDialogMode.VERIFY -> {
                val success = authManager.verifyPin(pin)
                if (success) {
                    callback?.onPinSuccess(PinDialogMode.VERIFY)
                    dismiss()
                } else {
                    val remaining = lockoutManager.getRemainingAttempts()
                    showErrorFeedback("Incorrect PIN. $remaining attempts remaining.")
                }
            }
            PinDialogMode.RESET -> {
                val success = authManager.verifyPin(pin)
                if (success) {
                    currentMode = PinDialogMode.SETUP
                    enteredPin.clear()
                    updateLabelsForMode()
                    updatePinDots()
                    updateConfirmButtonState()
                } else {
                    val remaining = lockoutManager.getRemainingAttempts()
                    showErrorFeedback("Incorrect PIN. $remaining attempts remaining.")
                }
            }
            PinDialogMode.REMOVE -> {
                val success = authManager.verifyPin(pin)
                if (success) {
                    authManager.removePin()
                    callback?.onPinSuccess(PinDialogMode.REMOVE)
                    dismiss()
                } else {
                    val remaining = lockoutManager.getRemainingAttempts()
                    showErrorFeedback("Incorrect PIN. $remaining attempts remaining.")
                }
            }
        }
    }

    private fun showErrorFeedback(message: String? = null) {
        val shake = AnimationUtils.loadAnimation(context, R.anim.anim_shake)
        binding.dotContainer.startAnimation(shake)
        
        for (i in 0 until binding.dotContainer.childCount) {
            val dotView = binding.dotContainer.getChildAt(i)
            dotView.backgroundTintList = ColorStateList.valueOf(config.errorColor)
        }
        
        enteredPin.clear()
        updateConfirmButtonState()
        
        binding.dotContainer.postDelayed({
            if (isAdded) {
                if (lockoutManager.isLockedOut()) {
                    checkLockoutState()
                } else {
                    updatePinDots()
                }
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
}
