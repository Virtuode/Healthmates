package com.corps.healthmate.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Long = 1, // Single row for the user
    val level: Int = 1,
    val xp: Int = 0,
    val dailySteps: Int = 0,
    val totalSteps: Long = 0, // New: lifetime steps
    val totalCalories: Double = 0.0,
    val totalCarbonSaved: Double = 0.0,
    val streak: Int = 0,
    val lastStreakDate: Long = 0 // New: track streak continuity
)
