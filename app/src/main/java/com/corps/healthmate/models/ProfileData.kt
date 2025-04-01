package com.corps.healthmate.models

data class ProfileData(
    val basicInfo: BasicInfo? = null,
    val bloodGroup: BloodGroupInfo? = null,
    val currentHealth: CurrentHealthInfo? = null,
    val medicalHistory: MedicalHistory? = null,
    val emergencyContact: EmergencyContact? = null
) {
    data class BasicInfo(
        val firstName: String = "",
        val middleName: String = "", // Added for SurveyScreen
        val lastName: String = "",
        val age: Int? = null,
        val gender: String = "",
        val contactNumber: String = "",
        val height: Float = 0f,
        val weight: Float = 0f,
        val imageUrl: String? = null // Added for SurveyScreen
    )

    data class BloodGroupInfo(
        val bloodGroup: String = "",
        val rhFactor: String = ""
    )

    data class CurrentHealthInfo(
        val symptoms: List<String>? = null,
        val medications: List<Medication>? = null,
        val lifestyleHabits: List<String>? = null,
        val sleepDuration: Float = 0f,
        val exercise: String = "",
        val stressLevel: String = "" // Added for SurveyScreen
    ) {
        data class Medication(
            val name: String = "",
            val dosage: String = "",
            val frequency: String = ""
        )
    }

    data class MedicalHistory(
        val chronicConditions: List<String>? = null,
        val familyMedicalHistory: List<FamilyCondition>? = null
    ) {
        data class FamilyCondition(
            val relation: String = "",
            val condition: String = "",
            val details: String = ""
        )
    }

    data class EmergencyContact(
        val name: String = "",
        val relation: String = "",
        val phone: String = "",
        val email: String? = null // Added for SurveyScreen
    )
}