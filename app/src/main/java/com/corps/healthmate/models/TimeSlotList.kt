package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable

class TimeSlotList(val timeSlots: List<TimeSlot>) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.createTypedArrayList(TimeSlot.CREATOR) ?: emptyList())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeTypedList(timeSlots)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TimeSlotList> {
        override fun createFromParcel(parcel: Parcel): TimeSlotList {
            return TimeSlotList(parcel)
        }

        override fun newArray(size: Int): Array<TimeSlotList?> {
            return arrayOfNulls(size)
        }
    }
}