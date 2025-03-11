package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable
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
    val status: String = STATUS_PENDING,
    val amount: Int = 0,
    val paymentId: String = "",
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
    val patientName: String? = null,
    val patientPhone: String? = null,
    val patientEmail: String? = null,
    val rating: Float? = null,
    val doctorName: String = "", // Changed to non-nullable with default empty string
    val timestamp: Long = System.currentTimeMillis() // Already initialized, kept for clarity
) : Parcelable {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_CONFIRMED = "confirmed"
        const val STATUS_CANCELLED = "cancelled"
        const val STATUS_COMPLETED = "completed"

        @JvmField
        val CREATOR = object : Parcelable.Creator<Appointment> {
            override fun createFromParcel(parcel: Parcel): Appointment {
                return Appointment(parcel)
            }

            override fun newArray(size: Int): Array<Appointment?> {
                return arrayOfNulls(size)
            }
        }
    }

    // Secondary constructor
    constructor(
        id: String = "",
        doctorId: String,
        patientId: String,
        appointmentDateTime: String,
        timeSlot: TimeSlot,
        amount: Int,
        paymentId: String,
        doctorName: String = "", // Added default value for consistency
        timestamp: Long = System.currentTimeMillis() // Added explicit parameter with default
    ) : this(
        id = id,
        doctorId = doctorId,
        patientId = patientId,
        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(appointmentDateTime)!!
        ),
        day = timeSlot.day,
        startTime = timeSlot.startTime,
        endTime = timeSlot.endTime,
        status = STATUS_CONFIRMED,
        amount = amount,
        paymentId = paymentId,
        doctorName = doctorName, // Pass through
        timestamp = timestamp // Pass through
    )

    // Parcelable implementation
    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(doctorId)
        parcel.writeString(patientId)
        parcel.writeString(date)
        parcel.writeString(day)
        parcel.writeString(startTime)
        parcel.writeString(endTime)
        parcel.writeString(status)
        parcel.writeInt(amount)
        parcel.writeString(paymentId)
        parcel.writeString(createdAt)
        parcel.writeString(patientName)
        parcel.writeString(patientPhone)
        parcel.writeString(patientEmail)
        parcel.writeValue(rating)
        parcel.writeString(doctorName) // Updated for non-nullable
        parcel.writeLong(timestamp)
    }

    private constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        doctorId = parcel.readString() ?: "",
        patientId = parcel.readString() ?: "",
        date = parcel.readString() ?: "",
        day = parcel.readString() ?: "",
        startTime = parcel.readString() ?: "",
        endTime = parcel.readString() ?: "",
        status = parcel.readString() ?: STATUS_PENDING,
        amount = parcel.readInt(),
        paymentId = parcel.readString() ?: "",
        createdAt = parcel.readString() ?: SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
        patientName = parcel.readString(),
        patientPhone = parcel.readString(),
        patientEmail = parcel.readString(),
        rating = parcel.readValue(Float::class.java.classLoader) as? Float,
        doctorName = parcel.readString() ?: "", // Updated for non-nullable
        timestamp = parcel.readLong()
    )
}