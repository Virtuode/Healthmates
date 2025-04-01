package com.corps.healthmate.models

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val earnedDate: Long
)