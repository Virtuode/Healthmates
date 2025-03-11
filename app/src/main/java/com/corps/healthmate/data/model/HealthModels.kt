package com.corps.healthmate.data.model



data class HealthProfile(
    val weight: Float,
    val height: Float,
    val age: Int,
    val gender: String,
    val medicalConditions: List<String>,
    val dietaryRestrictions: List<String>,
    val sleepGoal: Int, // in hours
    val waterGoal: Int, // in glasses
    val stepsGoal: Int
)

data class HealthSuggestion(
    val title: String,
    val description: String,
    val recommendations: List<String>
)

data class DailyProgress(
    val steps: Int,
    val waterGlasses: Int,
    val sleepHours: Float,
    val activeMinutes: Int,
    val achievements: List<Achievement>
)
