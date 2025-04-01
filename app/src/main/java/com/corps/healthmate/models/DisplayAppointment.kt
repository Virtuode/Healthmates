package com.corps.healthmate.models



data class DisplayAppointment(
    val appointment: Appointment,
    val doctorImageUrl: String,
    val doctorName: String? = null,
    val chatId: String? = null,
    val isVideoCallInitiated: Boolean? = null
)