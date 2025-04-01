// Notification.kt (com.corps.healthmate.models)
package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable

data class Notification(
    val id: String = "",
    val appointmentId: String = "",
    val type: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
) : Parcelable {
    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Notification> {
            override fun createFromParcel(parcel: Parcel): Notification {
                return Notification(parcel)
            }

            override fun newArray(size: Int): Array<Notification?> {
                return arrayOfNulls(size)
            }
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(appointmentId)
        parcel.writeString(type)
        parcel.writeString(message)
        parcel.writeLong(timestamp)
        parcel.writeByte(if (read) 1 else 0)
    }

    private constructor(parcel: Parcel) : this(
        id = parcel.readString() ?: "",
        appointmentId = parcel.readString() ?: "",
        type = parcel.readString() ?: "",
        message = parcel.readString() ?: "",
        timestamp = parcel.readLong(),
        read = parcel.readByte() != 0.toByte()
    )
}