package com.corps.healthmate.repository

import com.corps.healthmate.models.Appointment
import com.corps.healthmate.models.Notification
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import timber.log.Timber

class AppointmentRepository {
    private val database = FirebaseDatabase.getInstance().reference
    private val notificationsRef = database.child("notifications")

    fun getUpcomingAppointments(patientId: String, callback: (List<Appointment>) -> Unit) {
        database.child("patients/$patientId/appointments")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val currentTime = System.currentTimeMillis()
                    val missedThreshold = currentTime - (24 * 60 * 60 * 1000)
                    val appointments = mutableListOf<Appointment>()
                    for (child in snapshot.children) {
                        val appointment = child.getValue(Appointment::class.java)
                        if (appointment != null) {
                            val startTimeMillis = parseDateTime(appointment.date, appointment.startTime)
                            val endTimeMillis = parseDateTime(appointment.date, appointment.endTime)
                            if ((appointment.status == Appointment.STATUS_CONFIRMED && startTimeMillis > currentTime) ||
                                (appointment.status == "missed" && endTimeMillis > missedThreshold)) {
                                appointments.add(appointment)
                            }
                        }
                    }
                    Timber.d("Fetched upcoming appointments for patient $patientId: $appointments")
                    callback(appointments.sortedBy { it.date })
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Failed to fetch upcoming appointments: ${error.message}")
                    callback(emptyList())
                }
            })
    }

    suspend fun getAllAppointments(patientId: String): List<Appointment> = suspendCancellableCoroutine { continuation ->
        database.child("patients/$patientId/appointments")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val appointments = snapshot.children.mapNotNull { it.getValue(Appointment::class.java) }
                    Timber.d("Fetched all appointments for patient $patientId: $appointments")
                    continuation.resume(appointments)
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Failed to fetch all appointments: ${error.message}")
                    continuation.resumeWithException(error.toException())
                }
            })
    }

    suspend fun updateAppointmentStatus(appointmentId: String, newStatus: String) {
        database.child("appointments/$appointmentId/status").setValue(newStatus).await()
    }

    suspend fun rescheduleAppointment(appointmentId: String, newDate: String, newStartTime: String, newEndTime: String) {
        val updates = mapOf(
            "date" to newDate,
            "startTime" to newStartTime,
            "endTime" to newEndTime
        )
        database.child("appointments/$appointmentId").updateChildren(updates).await()
    }

    suspend fun insertNotification(notification: Notification) {
        notificationsRef.child(notification.id).setValue(notification).await()
    }

    fun getAllNotifications(callback: (List<Notification>) -> Unit) {
        notificationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val notifications = snapshot.children.mapNotNull { it.getValue(Notification::class.java) }
                Timber.d("Fetched all notifications: $notifications")
                callback(notifications)
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Failed to fetch notifications: ${error.message}")
                callback(emptyList())
            }
        })
    }

    suspend fun markNotificationAsRead(notificationId: String) {
        notificationsRef.child(notificationId).child("read").setValue(true).await()
    }

    private fun parseDateTime(date: String, time: String): Long {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return try {
            formatter.parse("$date $time")?.time ?: 0L
        } catch (e: Exception) {
            Timber.e(e, "Error parsing date and time: $date $time")
            0L
        }
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(task.exception ?: Exception("Task failed"))
        }
    }
}