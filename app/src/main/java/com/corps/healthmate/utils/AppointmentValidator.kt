package com.corps.healthmate.utils

import com.corps.healthmate.models.TimeSlot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object AppointmentValidator {
    suspend fun isTimeSlotAvailable(
        userId: String,
        selectedDate: String,
        selectedTime: String,
        selectedDay: String,
        doctorId: String
    ): Boolean {
        val database = FirebaseDatabase.getInstance().reference
        try {
            val snapshot = database.child("doctors").child(doctorId)
                .child("selectedTimeSlots").get().await()
            if (!snapshot.exists()) return false

            val timeSlots = snapshot.children.mapNotNull { it.getValue(TimeSlot::class.java) }
            return timeSlots.any { timeSlot ->
                timeSlot.day == selectedDay &&
                        timeSlot.startTime == selectedTime &&
                        timeSlot.isAvailable == true
            }
        } catch (e: Exception) {
            println("Error checking time slot availability: ${e.message}")
            return false
        }
    }
}