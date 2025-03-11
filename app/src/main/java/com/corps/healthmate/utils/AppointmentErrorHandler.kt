package com.corps.healthmate.utils

import android.content.Context
import android.widget.Toast
import com.corps.healthmate.R

object AppointmentErrorHandler {
    fun handleBookingError(context: Context, error: AppError) {
        val message = when (error) {
            is AppError.NetworkError -> context.getString(R.string.network_error_message)
            is AppError.ValidationError -> error.message
            is AppError.PaymentError -> "Payment failed: ${error.message}"
            is AppError.DatabaseError -> "Database error: ${error.message}"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    sealed class AppError {
        object NetworkError : AppError()
        data class ValidationError(val message: String) : AppError()
        data class PaymentError(val message: String) : AppError()
        data class DatabaseError(val message: String) : AppError()
    }
}