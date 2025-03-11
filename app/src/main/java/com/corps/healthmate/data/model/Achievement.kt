package com.corps.healthmate.data.model

data class Achievement(
    val title: String,
    val description: String,
    val points: Int,
    val iconResId: Int = 0,
    val progress: Int = 0,
    val maxProgress: Int = 100,
    val isUnlocked: Boolean = false
)
