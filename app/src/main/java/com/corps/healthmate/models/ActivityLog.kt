package com.corps.healthmate.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "activity_log")
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val steps: Int,
    val timestamp: Long
)