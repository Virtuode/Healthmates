package com.corps.healthmate.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.corps.healthmate.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class VerificationBottomSheetDialog : BottomSheetDialogFragment() {
    private lateinit var verificationStatusTextView: TextView
    private lateinit var verificationMessageTextView: TextView
    private val auth = FirebaseAuth.getInstance()
    private var verificationListener: VerificationListener? = null

    interface VerificationListener {
        fun onVerificationDone()
        fun onLoginRequested()
    }

    fun setVerificationListener(listener: VerificationListener) {
        this.verificationListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification_bottom_sheet_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        verificationStatusTextView = view.findViewById(R.id.verificationStatusTextView)
        verificationMessageTextView = view.findViewById(R.id.verificationMessageTextView)

        // Apply entry animations
        applyEntryAnimations(view)

        // Start checking verification status
        startVerificationCheck()
    }

    private fun applyEntryAnimations(view: View) {
        // Slide up animation
        val slideUp = ObjectAnimator.ofFloat(view, "translationY", 300f, 0f)
        slideUp.duration = 500

        // Fade in animation
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        fadeIn.duration = 500

        // Combine animations
        val animSet = AnimatorSet()
        animSet.playTogether(slideUp, fadeIn)
        animSet.start()
    }

    private fun startVerificationCheck() {
        try {
            val user = auth.currentUser
            if (user == null) {
                handleVerificationError("User not found")
                return
            }

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        user.reload().await()
                    }
                    
                    if (!isAdded) return@launch

                    if (user.isEmailVerified) {
                        handleVerificationSuccess()
                    } else {
                        handleVerificationPending()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error during verification check")
                    handleVerificationError(e.message ?: "Verification check failed")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error initializing verification check")
            handleVerificationError(e.message ?: "Verification initialization failed")
        }
    }

    private fun handleVerificationSuccess() {
        if (!isAdded) return
        
        verificationStatusTextView.text = "Email Verified"
        verificationStatusTextView.setTextColor(resources.getColor(R.color.logo_color, null))
        verificationMessageTextView.text = "Your email has been verified successfully! Redirecting..."

        lifecycleScope.launch {
            try {
                delay(2000)
                if (isAdded && !requireActivity().isFinishing) {
                    verificationListener?.onVerificationDone()
                    dismissAllowingStateLoss()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during verification success handling")
            }
        }
    }

    private fun handleVerificationPending() {
        if (!isAdded) return

        verificationStatusTextView.text = "Email Not Verified"
        verificationStatusTextView.setTextColor(resources.getColor(R.color.hint_color, null))
        verificationMessageTextView.text = "Please verify your email to continue. Check your inbox."
        
        lifecycleScope.launch {
            try {
                delay(3000)
                if (isAdded && !requireActivity().isFinishing) {
                    startVerificationCheck()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error scheduling next verification check")
            }
        }
    }

    private fun handleVerificationError(errorMessage: String) {
        if (!isAdded) return

        Timber.e("Verification error: $errorMessage")
        verificationStatusTextView.text = "Verification Error"
        verificationStatusTextView.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        verificationMessageTextView.text = "An error occurred. Please try again later."
        
        lifecycleScope.launch {
            delay(2000)
            if (isAdded && !requireActivity().isFinishing) {
                verificationListener?.onLoginRequested()
                dismissAllowingStateLoss()
            }
        }
    }

    override fun onDestroy() {
        try {
            super.onDestroy()
            if (auth.currentUser?.isEmailVerified != true) {
                verificationListener?.onLoginRequested()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in onDestroy")
        }
    }

    companion object {
        const val TAG = "VerificationBottomSheet"
    }
}