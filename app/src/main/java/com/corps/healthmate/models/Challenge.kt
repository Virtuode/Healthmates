package com.corps.healthmate.models



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "challenges")
data class Challenge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val goalSteps: Int,
    var currentSteps: Int = 0,
    var status: String = "active", // New: "active", "completed"
    val rewardXp: Int = 100 // New: XP reward on completion
) {
    val progress: Int
        get() = if (goalSteps > 0) (currentSteps * 100 / goalSteps) else 0
}