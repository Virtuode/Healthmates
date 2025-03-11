package com.corps.healthmate.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.corps.healthmate.utils.ListConverter

@Entity(tableName = "reminders")
@TypeConverters(ListConverter::class)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var userId: String,
    var pillNames: List<String>,
    var time: String,
    var day: String,
    var dosage: String? = null,
    var ringtoneUri: String? = null,
    var isActive: Boolean = true,
    var isHindi: Boolean = false,
    var lastTakenTime: Long = 0,
    var nextTriggerTime: Long = 0,
    var frequency: String = "daily" // Can be "once", "daily", "weekly", "monthly"
)