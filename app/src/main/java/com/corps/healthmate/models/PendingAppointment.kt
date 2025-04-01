package com.corps.healthmate.models

data class PendingAppointment(
    val id: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val status: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val timeSlot: String = "", // Added to match Appointment
    val amount: Int = 0, // Added to match Appointment
    val paymentId: String = "", // Added to match Appointment
    val createdAt: String = "", // Added to match Appointment
    val consultationType: String? = null // Added to match Appointment
)