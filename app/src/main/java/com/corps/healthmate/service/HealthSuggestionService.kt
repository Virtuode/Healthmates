package com.corps.healthmate.service

import android.content.Context
import com.corps.healthmate.data.model.HealthProfile
import com.corps.healthmate.data.model.HealthSuggestion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthSuggestionService @Inject constructor(
    private val context: Context
) {
    fun generateDietSuggestions(profile: HealthProfile): List<HealthSuggestion> {
        val suggestions = mutableListOf<HealthSuggestion>()
        
        // Based on BMI
        val bmi = calculateBMI(profile.weight, profile.height)
        when {
            bmi < 18.5 -> suggestions.add(
                HealthSuggestion(
                    "Weight Gain Diet",
                    "Include protein-rich foods, healthy fats, and complex carbohydrates",
                    listOf("Eggs", "Nuts", "Avocados", "Whole grains")
                )
            )
            bmi > 25 -> suggestions.add(
                HealthSuggestion(
                    "Weight Management Diet",
                    "Focus on portion control and nutrient-dense foods",
                    listOf("Lean proteins", "Vegetables", "Fruits", "Whole grains")
                )
            )
        }

        // Based on medical conditions
        profile.medicalConditions.forEach { condition ->
            when (condition.lowercase()) {
                "diabetes" -> suggestions.add(
                    HealthSuggestion(
                        "Diabetic-Friendly Diet",
                        "Low glycemic index foods and balanced meals",
                        listOf("Leafy greens", "Whole grains", "Lean proteins", "Healthy fats")
                    )
                )
                "hypertension" -> suggestions.add(
                    HealthSuggestion(
                        "Low Sodium Diet",
                        "Reduce sodium intake and increase potassium-rich foods",
                        listOf("Fruits", "Vegetables", "Low-fat dairy", "Lean proteins")
                    )
                )
            }
        }

        return suggestions
    }

    fun generateSleepSuggestions(
        profile: HealthProfile,
        phoneUsage: Int, // in minutes
        bedtime: String,
        symptoms: List<String>
    ): List<HealthSuggestion> {
        val suggestions = mutableListOf<HealthSuggestion>()

        // Phone usage before bed
        if (phoneUsage > 60) {
            suggestions.add(
                HealthSuggestion(
                    "Reduce Screen Time",
                    "High phone usage before bed can affect sleep quality",
                    listOf("Use blue light filter", "Stop using phone 1 hour before bed")
                )
            )
        }

        // Bedtime routine
        val bedtimeHour = bedtime.split(":")[0].toInt()
        when {
            bedtimeHour < 21 -> suggestions.add(
                HealthSuggestion(
                    "Adjust Bedtime",
                    "Your current bedtime might be too early",
                    listOf("Try going to bed between 9 PM and 11 PM")
                )
            )
            bedtimeHour > 23 -> suggestions.add(
                HealthSuggestion(
                    "Earlier Bedtime",
                    "Late bedtime might affect sleep quality",
                    listOf("Try going to bed 30 minutes earlier", "Maintain consistent sleep schedule")
                )
            )
        }

        // Symptom-based suggestions
        symptoms.forEach { symptom ->
            when (symptom.lowercase()) {
                "insomnia" -> suggestions.add(
                    HealthSuggestion(
                        "Insomnia Management",
                        "Natural ways to improve sleep quality",
                        listOf(
                            "Practice relaxation techniques",
                            "Create a bedtime routine",
                            "Avoid caffeine after 2 PM"
                        )
                    )
                )
                "anxiety" -> suggestions.add(
                    HealthSuggestion(
                        "Anxiety and Sleep",
                        "Managing anxiety for better sleep",
                        listOf(
                            "Try meditation before bed",
                            "Practice deep breathing",
                            "Write in a journal"
                        )
                    )
                )
            }
        }

        return suggestions
    }

    private fun calculateBMI(weight: Float, height: Float): Float {
        return weight / (height * height)
    }
}
