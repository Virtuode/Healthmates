package com.corps.healthmate.data

data class ProfileData(
    val basicInfo: BasicInfo? = null,
    val bloodGroup: BloodGroupInfo? = null,
    val currentHealth: CurrentHealthInfo? = null,
    val medicalHistory: MedicalHistory? = null,
    val emergencyContact: EmergencyContact? = null
) {
    data class BasicInfo(
        val firstName: String = "",
        val middleName: String = "",
        val lastName: String = "",
        val age: Int? = null,
        val gender: String = "",
        val contactNumber: String = "",
        val height: Float? = null,
        val weight: Float? = null,
        val imageUrl: String? = null
    )

    data class BloodGroupInfo(
        val bloodGroup: String = "",
        val rhFactor: String = ""
    )

    data class CurrentHealthInfo(
        val symptoms: List<String> = emptyList(),
        val medications: List<Medication> = emptyList(),
        val lifestyleHabits: List<String> = emptyList(),
        val sleepDuration: Float = 0f,
        val exercise: String = "",
        val stressLevel: String = ""
    ) {
        data class Medication(
            val name: String,
            val dosage: String,
            val frequency: String
        )
    }

    data class MedicalHistory(
        val chronicConditions: List<String> = emptyList(),
        val familyMedicalHistory: List<FamilyCondition> = emptyList()
    ) {
        data class FamilyCondition(
            val relation: String,
            val condition: String,
            val details: String
        )
    }

    data class EmergencyContact(
        val name: String = "",
        val relation: String = "",
        val phone: String = "",
        val email: String = ""
    )
} 