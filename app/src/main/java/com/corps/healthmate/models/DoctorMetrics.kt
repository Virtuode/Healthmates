package com.corps.healthmate.models

data class DoctorMetrics(
    val totalAppointments: Int = 0,
    val completedAppointments: Int = 0,
    val cancelledAppointments: Int = 0,
    val averageRating: Double = 0.0,
    val mostPopularTimeSlots: String? = null
) 