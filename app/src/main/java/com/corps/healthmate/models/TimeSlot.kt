package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable

data class TimeSlot(
    val day: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val id: String = "",
    var isAvailable: Boolean = true
) : Parcelable {

    private constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt() == 1
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