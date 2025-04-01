package com.corps.healthmate.models

import android.os.Parcel
import android.os.Parcelable

data class DoctorSummary(
    val id: String? = "",
    val name: String? = null,
    val specialization: String? = null,
    val experience: String? = null,
    val education: String? = null,
    val availableDays: List<String> = emptyList(),
    val biography: String? = null,
    val email: String? = null,
    val languages: List<String>? = null,
    val phone: String? = null,
    val imageUrl: String? = null,
    val selectedTimeSlots: List<TimeSlot> = emptyList(),
    val isVerified: Boolean? = false,
    val createdAt: String? = null,
    val documentUrl: String? = null,
    val gender: String? = null,
    val licenseNumber: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList() ?: emptyList(),
        parcel.readString(),
        parcel.readString(),
        parcel.createStringArrayList(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(TimeSlot.CREATOR)?.toList() ?: emptyList(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(specialization)
        parcel.writeString(experience)
        parcel.writeString(education)
        parcel.writeStringList(availableDays)
        parcel.writeString(biography)
        parcel.writeString(email)
        parcel.writeStringList(languages)
        parcel.writeString(phone)
        parcel.writeString(imageUrl)
        parcel.writeTypedList(selectedTimeSlots)
        parcel.writeByte(if (isVerified == true) 1 else 0)
        parcel.writeString(createdAt)
        parcel.writeString(documentUrl)
        parcel.writeString(gender)
        parcel.writeString(licenseNumber)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<DoctorSummary> {
        override fun createFromParcel(parcel: Parcel): DoctorSummary = DoctorSummary(parcel)
        override fun newArray(size: Int): Array<DoctorSummary?> = arrayOfNulls(size)
    }
}