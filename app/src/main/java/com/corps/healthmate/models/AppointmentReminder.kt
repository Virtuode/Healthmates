package com.corps.healthmate.models

data class AppointmentReminder(
    val id: String,
    val doctorName: String,
    val type: String,
    val timestamp: Long,
    val isConfirmed: Boolean = false,
    val notes: String? = null
)
