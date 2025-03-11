package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable

data class TimeSlot(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val id: String = "",
    val isAvailable: Boolean = true
) : Parcelable {
    constructor() : this("", "", "", "", true)

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readByte() != 0.toByte()
    )

    fun toMap(): Map<String, Any> = mapOf(
        "day" to day,
        "startTime" to startTime,
        "endTime" to endTime,
        "id" to id,
        "isAvailable" to isAvailable
    )

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(day)
        dest.writeString(startTime)
        dest.writeString(endTime)
        dest.writeString(id)
        dest.writeByte(if (isAvailable) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<TimeSlot> {
        override fun createFromParcel(parcel: Parcel): TimeSlot = TimeSlot(parcel)
        override fun newArray(size: Int): Array<TimeSlot?> = arrayOfNulls(size)
    }
}