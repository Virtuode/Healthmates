package com.corps.healthmate.models

data class ActivityStats(
    val steps: Int,
    val calories: Double,
    val carbonSaved: Double,
    val duration: Long,
    val date: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
) {
    companion object {
        // Constants for calculations
        private const val CALORIES_PER_STEP = 0.04 // Average calories burned per step
        private const val CARBON_SAVED_PER_KM = 0.2 // kg CO2 saved per km walked vs driving
        private const val STEPS_PER_KM = 1312 // Average steps per kilometer

        fun calculateStats(steps: Int, duration: Long, isCompleted: Boolean = false): ActivityStats {
            // Ensure steps is non-negative
            require(steps >= 0) { "Steps cannot be negative" }

            // Calculate calories burned
            val calories = steps * CALORIES_PER_STEP
            
            // Calculate distance in kilometers
            val distanceInKm = steps.toDouble() / STEPS_PER_KM
            
            // Calculate carbon saved
            val carbonSaved = distanceInKm * CARBON_SAVED_PER_KM

            // Return the ActivityStats object with calculated values
            return ActivityStats(
                steps = steps,
                calories = calories,
                carbonSaved = carbonSaved,
                duration = duration,
                isCompleted = isCompleted
            )
        }
    }
} 