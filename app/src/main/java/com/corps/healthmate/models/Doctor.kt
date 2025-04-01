package com.corps.healthmate.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Doctor(
    val id: String = "",
    val name: String = "",
    val profilePicture: String? = null,
    val specialization: String? = null,
    val experience: Int = 0,
    val email: String? = null,
    val phone: String? = null
) : Parcelable
