data class AppointmentFeedback(
    val appointmentId: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val rating: Float = 0f,
    val feedback: String = "",
    val timestamp: Long = System.currentTimeMillis()
) 