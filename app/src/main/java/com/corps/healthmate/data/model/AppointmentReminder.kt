package com.corps.healthmate.data.model

data class AppointmentReminder(
    val id: String,
    val doctorName: String,
    val type: String,
    val timestamp: Long,
    val isConfirmed: Boolean = false,
    val notes: String? = null
)
