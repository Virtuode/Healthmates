package com.corps.healthmate.models

import java.text.SimpleDateFormat
import java.util.*

/**
 * Model class representing a chat between a patient and a doctor.
 */
data class Chat(
    val id: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val appointmentId: String = "",
    val appointmentTime: String = "",
    val doctorName: String = "",
    val doctorImageUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val unreadCount: Int = 0,
    val isActive: Boolean = false,
    val status: String = "pending"
) {
    val remainingDays: Int
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentDate = sdf.parse(appointmentTime) ?: return Int.MAX_VALUE
            val currentTime = Date()
            val diff = appointmentDate.time - currentTime.time
            return (diff / (1000 * 60 * 60 * 24)).toInt()
        }
}

data class Message(
    val id: String = "",
    val senderId: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text", // text, image, etc.
    val status: String = "sent" // sent, delivered, read
)