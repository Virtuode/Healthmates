package com.corps.healthmate.utils

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.corps.healthmate.R
import com.corps.healthmate.activities.LoginActivity

sealed class AppError(val message: String) {
    class NetworkError(message: String) : AppError(message)
    class ValidationError(message: String) : AppError(message)
    class AuthenticationError(message: String) : AppError(message)
    class AppointmentError(message: String) : AppError(message)
}

object ErrorHandler {
    fun handleError(error: AppError, context: Context) {
        when (error) {
            is AppError.NetworkError -> showNetworkError(context)
            is AppError.ValidationError -> showValidationError(context, error.message)
            is AppError.AuthenticationError -> handleAuthError(context)
            is AppError.AppointmentError -> handleAppointmentError(context, error.message)
        }
    }

    private fun showNetworkError(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.network_error_title)
            .setMessage(R.string.network_error_message)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showValidationError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun handleAuthError(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(R.string.auth_error_title)
            .setMessage(R.string.auth_error_message)
            .setPositiveButton(R.string.login) { _, _ ->
                // Redirect to login screen
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            }
            .setCancelable(false)
            .show()
    }

    private fun handleAppointmentError(context: Context, message: String) {
        AlertDialog.Builder(context)
            .setTitle(R.string.appointment_error_title)
            .setMessage(message)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .show()
    }
} 