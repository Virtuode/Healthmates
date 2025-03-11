package com.corps.healthmate.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.corps.healthmate.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordBottomSheet : BottomSheetDialogFragment() {
    private lateinit var emailEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var cancelButton: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.forgot_password_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        emailEditText = view.findViewById(R.id.forgot_password_email)
        resetButton = view.findViewById(R.id.reset_password_button)
        cancelButton = view.findViewById(R.id.cancel_reset)

        // Apply animations
        applyEntryAnimations(view)

        // Set click listeners
        resetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isEmpty()) {
                emailEditText.error = "Email is required"
                return@setOnClickListener
            }

            resetPassword(email)
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun applyEntryAnimations(view: View) {
        // Slide up animation for the entire sheet
        val slideUp = ObjectAnimator.ofFloat(view, "translationY", 300f, 0f)
        slideUp.duration = 500

        // Fade in animation for the entire sheet
        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        fadeIn.duration = 500

        // Combine animations
        val animSet = AnimatorSet()
        animSet.playTogether(slideUp, fadeIn)
        animSet.start()
    }

    private fun resetPassword(email: String) {
        resetButton.isEnabled = false
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Password reset email sent. Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                } else {
                    val error = task.exception?.message ?: "Failed to send reset email"
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    Log.e("ForgotPassword", "Error: ${task.exception}")
                }
                resetButton.isEnabled = true
            }
    }
}