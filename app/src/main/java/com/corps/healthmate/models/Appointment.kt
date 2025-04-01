package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

data class Appointment(
    val id: String = "",
    val doctorId: String = "",
    val patientId: String = "",
    val date: String = "",
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val timeSlot: String = "",
    val status: String = STATUS_PENDING,
    val amount: Int = 0,
    val paymentId: String = "",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val patientName: String? = null,
    val patientPhone: String? = null,
    val patientEmail: String? = null,
    val rating: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val consultationType: String? = null
) : Parcelable {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_ONGOING = "ongoing"
        const val STATUS_COMPLETED = "completed"
        const val STATUS_MISSED = "missed"

        @JvmField
        val CREATOR = object : Parcelable.Creator<Appointment> {
            override fun createFromParcel(parcel: Parcel): Appointment = Appointment(parcel)
            override fun newArray(size: Int): Array<Appointment?> = arrayOfNulls(size)
        }
    }

    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id, "doctorId" to doctorId, "patientId" to patientId, "date" to date, "day" to day,
        "startTime" to startTime, "endTime" to endTime, "timeSlot" to startTime, "status" to status,
        "amount" to amount, "paymentId" to paymentId, "createdAt" to createdAt, "consultationType" to consultationType
    )

    fun getTimeRemaining(currentDate: Date): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val appointmentDateTime = sdf.parse("$date $startTime") ?: return "Invalid Date"
            val diffInMillis = appointmentDateTime.time - currentDate.time
            return if (diffInMillis <= 0) "Expired" else {
                val minutes = (diffInMillis / 1000 / 60).toInt()
                when {
                    minutes < 60 -> "$minutes min"
                    minutes < 1440 -> "${minutes / 60} hr"
                    else -> "${minutes / 1440} days"
                }
            }
        } catch (e: Exception) {
            return "Invalid Date"
        }
    }

    fun isOngoing(currentDate: Date): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        try {
            val startDateTime = sdf.parse("$date $startTime") ?: return false
            val endDateTime = sdf.parse("$date $endTime") ?: return false
            return currentDate.after(startDateTime) && currentDate.before(endDateTime)
        } catch (e: Exception) {
            Timber.e(e, "Error parsing appointment times")
            return false
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id); parcel.writeString(doctorId); parcel.writeString(patientId); parcel.writeString(date);
        parcel.writeString(day); parcel.writeString(startTime); parcel.writeString(endTime); parcel.writeString(timeSlot);
        parcel.writeString(status); parcel.writeInt(amount); parcel.writeString(paymentId); parcel.writeString(createdAt);
        parcel.writeString(patientName); parcel.writeString(patientPhone); parcel.writeString(patientEmail);
        parcel.writeValue(rating); parcel.writeLong(timestamp); parcel.writeString(consultationType);
    }

    private constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "", doctorId = parcel.readString() ?: "", patientId = parcel.readString() ?: "",
        date = parcel.readString() ?: "", day = parcel.readString() ?: "", startTime = parcel.readString() ?: "",
        endTime = parcel.readString() ?: "", timeSlot = parcel.readString() ?: "", status = parcel.readString() ?: STATUS_PENDING,
        amount = parcel.readInt(), paymentId = parcel.readString() ?: "", createdAt = parcel.readString() ?: "",
        patientName = parcel.readString(), patientPhone = parcel.readString(), patientEmail = parcel.readString(),
        rating = parcel.readValue(Float::class.java.classLoader) as? Float, timestamp = parcel.readLong(), consultationType = parcel.readString()
    )
}