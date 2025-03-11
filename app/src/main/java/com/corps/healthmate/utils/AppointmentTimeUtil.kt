package com.corps.healthmate.utils

import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object AppointmentTimeUtil {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    data class TimeRemaining(
        val days: Long = 0,
        val hours: Long = 0,
        val minutes: Long = 0,
        val seconds: Long = 0,
        val isUpcoming: Boolean = true
    )

    fun getTimeRemaining(appointmentTime: String): TimeRemaining {
        try {
            val appointmentDate = dateFormat.parse(appointmentTime) ?: return TimeRemaining()
            val currentTime = System.currentTimeMillis()
            val timeDiff = appointmentDate.time - currentTime

            // If timeDiff is negative, appointment has passed
            if (timeDiff < 0) {
                return TimeRemaining(isUpcoming = false)
            }

            // Convert milliseconds to days, hours, minutes, seconds
            val seconds = abs(timeDiff / 1000)
            return TimeRemaining(
                days = seconds / (24 * 3600),
                hours = (seconds % (24 * 3600)) / 3600,
                minutes = (seconds % 3600) / 60,
                seconds = seconds % 60,
                isUpcoming = true
            )
        } catch (e: Exception) {
            return TimeRemaining()
        }
    }

    fun formatTimeRemaining(timeRemaining: TimeRemaining): String {
        return when {
            !timeRemaining.isUpcoming -> "Appointment has passed"
            timeRemaining.days > 0 -> "${timeRemaining.days} days ${timeRemaining.hours}h remaining"
            timeRemaining.hours > 0 -> "${timeRemaining.hours}h ${timeRemaining.minutes}m remaining"
            timeRemaining.minutes > 0 -> "${timeRemaining.minutes}m remaining"
            else -> "Less than a minute remaining"
        }
    }

    fun isAppointmentNear(appointmentTime: String, thresholdMinutes: Int = 15): Boolean {
        try {
            val appointmentDate = dateFormat.parse(appointmentTime) ?: return false
            val currentTime = System.currentTimeMillis()
            val timeDiff = appointmentDate.time - currentTime
            
            // Convert to minutes
            val minutesRemaining = timeDiff / (60 * 1000)
            
            return minutesRemaining in 0..thresholdMinutes
        } catch (e: Exception) {
            return false
        }
    }
} 