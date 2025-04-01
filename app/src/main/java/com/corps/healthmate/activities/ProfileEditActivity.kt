package com.corps.healthmate.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.material.chip.Chip
import com.corps.healthmate.adapters.FamilyHistoryAdapter
import com.corps.healthmate.adapters.MedicationAdapter
import com.corps.healthmate.models.ProfileData
import com.corps.healthmate.databinding.ActivityProfileEditBinding
import com.corps.healthmate.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ProfileEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileEditBinding
    private var currentProfileData: ProfileData? = null
    private val familyMedicalHistory = mutableListOf<Map<String, String>>()
    private lateinit var medicationAdapter: MedicationAdapter
    private lateinit var familyHistoryAdapter: FamilyHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupExpandableCards()
        setupGenderDropdown()
        setupSaveButton()
        setupShareButton()
        setupFamilyMedicalHistorySection()
        setupRecyclerViews()
        setupAddMedicationButton()

        fetchProfileData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupExpandableCards() {
        binding.profileMainExpandButton.setOnClickListener {
            toggleVisibility(binding.profileMainContent)
            rotateExpandButton(binding.profileMainExpandButton)
        }
        binding.basicInfoExpandButton.setOnClickListener {
            toggleVisibility(binding.basicInfoContent)
            rotateExpandButton(binding.basicInfoExpandButton)
        }
        binding.currentHealthExpandButton.setOnClickListener {
            toggleVisibility(binding.currentHealthContent)
            rotateExpandButton(binding.currentHealthExpandButton)
        }
        binding.medicalHistoryExpandButton.setOnClickListener {
            toggleVisibility(binding.medicalHistoryContent)
            rotateExpandButton(binding.medicalHistoryExpandButton)
        }
        binding.emergencyContactExpandButton.setOnClickListener {
            toggleVisibility(binding.emergencyContactContent)
            rotateExpandButton(binding.emergencyContactExpandButton)
        }
    }

    private fun toggleVisibility(view: View) {
        view.visibility = if (view.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun rotateExpandButton(button: ImageButton) {
        button.animate().rotation(if (button.rotation == 0f) 180f else 0f).start()
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, genders)
        binding.genderDropdown.setAdapter(adapter)
    }

    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener { updateProfile() }
    }

    private fun setupShareButton() {
        binding.shareReportButton.setOnClickListener { sharePatientReport() }
    }

    private fun setupFamilyMedicalHistorySection() {
        binding.addFamilyMedicalHistoryButton.setOnClickListener {
            val relation = binding.familyRelationInput.text.toString()
            val condition = binding.familyConditionInput.text.toString()
            val details = binding.familyMedicalHistoryDetailsInput.text.toString()
            if (relation.isNotEmpty() && condition.isNotEmpty()) {
                familyMedicalHistory.add(mapOf("relation" to relation, "condition" to condition, "details" to details))
                familyHistoryAdapter.updateItems(familyMedicalHistory)
                binding.familyRelationInput.text?.clear()
                binding.familyConditionInput.text?.clear()
                binding.familyMedicalHistoryDetailsInput.text?.clear()
            } else {
                Toast.makeText(this, "Please fill in relation and condition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerViews() {
        medicationAdapter = MedicationAdapter { position ->
            medicationAdapter.updateItems(medicationAdapter.getMedications().toMutableList().apply { removeAt(position) })
        }
        binding.medicationsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.medicationsRecyclerView.adapter = medicationAdapter

        familyHistoryAdapter = FamilyHistoryAdapter { position ->
            familyMedicalHistory.removeAt(position)
            familyHistoryAdapter.updateItems(familyMedicalHistory)
        }
        binding.familyHistoryRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.familyHistoryRecyclerView.adapter = familyHistoryAdapter
    }

    private fun setupAddMedicationButton() {
        binding.addMedicationButton.setOnClickListener {
            // Add an empty medication entry for the user to fill
            val newMedication = ProfileData.CurrentHealthInfo.Medication("", "", "")
            medicationAdapter.updateItems(medicationAdapter.getMedications() + newMedication)
        }
    }

    private fun fetchProfileData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            showErrorMessage("User not logged in.")
            android.util.Log.e("ProfileEdit", "User ID is null, cannot fetch profile data")
            return
        }

        FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(userId)
            .child("survey")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentProfileData = snapshot.getValue(ProfileData::class.java)
                    android.util.Log.d("ProfileEdit", "Raw Snapshot: ${snapshot.value}")
                    android.util.Log.d("ProfileEdit", "Fetched Profile Data: $currentProfileData")
                    currentProfileData?.let { populateFields(it) }
                        ?: android.util.Log.w("ProfileEdit", "No profile data found in Firebase")
                }
                override fun onCancelled(error: DatabaseError) {
                    showErrorMessage("Failed to load profile: ${error.message}")
                    android.util.Log.e("ProfileEdit", "Firebase fetch cancelled: ${error.message}")
                }
            })
    }

    private fun populateFields(profileData: ProfileData) {
        profileData.basicInfo?.let {
            binding.firstNameInput.setText(it.firstName)
            binding.lastNameInput.setText(it.lastName)
            binding.ageInput.setText(it.age?.toString())
            binding.genderDropdown.setText(it.gender, false)
            binding.phoneInput.setText(it.contactNumber)
            binding.heightSlider.value = it.height
            binding.weightSlider.value = it.weight
        }

        profileData.bloodGroup?.let {
            val chipId = when ("${it.bloodGroup}${it.rhFactor}") {
                "A+" -> R.id.chipAPositive
                "A-" -> R.id.chipANegative
                "B+" -> R.id.chipBPositive
                "B-" -> R.id.chipBNegative
                "AB+" -> R.id.chipABPositive
                "AB-" -> R.id.chipABNegative
                "O+" -> R.id.chipOPositive
                "O-" -> R.id.chipONegative
                else -> R.id.chipOPositive // Default to O+ if not set
            }
            binding.bloodGroupChipGroup.check(chipId)
        } ?: run {
            binding.bloodGroupChipGroup.check(R.id.chipOPositive) // Default if null
        }

        profileData.currentHealth?.let {
            it.symptoms?.forEach { symptom ->
                binding.symptomsChipGroup.children.forEach { view ->
                    if (view is Chip && view.text.toString().equals(symptom, ignoreCase = true)) {
                        view.isChecked = true
                    }
                }
            }
            it.medications?.let { meds -> medicationAdapter.updateItems(meds) }
            it.lifestyleHabits?.forEach { habit ->
                binding.lifestyleChipGroup.children.forEach { view ->
                    if (view is Chip && view.text.toString().equals(habit, ignoreCase = true)) {
                        view.isChecked = true
                    }
                }
            }
            binding.sleepDurationSlider.value = it.sleepDuration
            when (it.exercise) {
                "Daily" -> binding.radioDaily.isChecked = true
                "3-4 times a week" -> binding.radio3Times.isChecked = true
                "Once a week" -> binding.radioOnce.isChecked = true
                "Rarely" -> binding.radioRarely.isChecked = true
                else -> binding.radioRarely.isChecked = true // Default
            }
        }

        profileData.medicalHistory?.let {
            it.chronicConditions?.forEach { condition ->
                binding.conditionsChipGroup.children.forEach { view ->
                    if (view is Chip && view.text.toString().equals(condition, ignoreCase = true)) {
                        view.isChecked = true
                    }
                }
                if (!listOf("Diabetes", "Hypertension", "Asthma", "Allergies").contains(condition)) {
                    binding.otherConditionsInput.setText(condition)
                }
            }
            it.familyMedicalHistory?.forEach { family ->
                familyMedicalHistory.add(mapOf(
                    "relation" to family.relation,
                    "condition" to family.condition,
                    "details" to family.details
                ))
            }
            familyHistoryAdapter.updateItems(familyMedicalHistory)
        }

        profileData.emergencyContact?.let {
            binding.emergencyNameInput.setText(it.name)
            binding.emergencyRelationInput.setText(it.relation)
            binding.emergencyPhoneInput.setText(it.phone)
        }
    }

    private fun updateProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        val updates = mutableMapOf<String, Any>()
        val profileData = getCurrentProfileDataFromUI()

        updates["basicInfo"] = profileData.basicInfo?.let {
            mapOf(
                "firstName" to it.firstName,
                "lastName" to it.lastName,
                "age" to it.age,
                "gender" to it.gender,
                "contactNumber" to it.contactNumber,
                "height" to it.height,
                "weight" to it.weight,
                "imageUrl" to (currentProfileData?.basicInfo?.imageUrl ?: "") // Preserve existing imageUrl
            )
        } ?: emptyMap<String, Any>()

        updates["bloodGroup"] = profileData.bloodGroup?.let {
            mapOf(
                "bloodGroup" to it.bloodGroup,
                "rhFactor" to it.rhFactor
            )
        } ?: emptyMap<String, Any>()

        updates["currentHealth"] = profileData.currentHealth?.let {
            mapOf(
                "symptoms" to (it.symptoms ?: emptyList<String>()),
                "medications" to (it.medications ?: emptyList<ProfileData.CurrentHealthInfo.Medication>()),
                "lifestyleHabits" to (it.lifestyleHabits ?: emptyList<String>()),
                "sleepDuration" to it.sleepDuration,
                "exercise" to it.exercise
            )
        } ?: emptyMap<String, Any>()

        updates["medicalHistory"] = profileData.medicalHistory?.let {
            mapOf(
                "chronicConditions" to (it.chronicConditions ?: emptyList<String>()),
                "familyMedicalHistory" to (it.familyMedicalHistory ?: emptyList<ProfileData.MedicalHistory.FamilyCondition>())
            )
        } ?: emptyMap<String, Any>()

        updates["emergencyContact"] = profileData.emergencyContact?.let {
            mapOf(
                "name" to it.name,
                "relation" to it.relation,
                "phone" to it.phone
            )
        } ?: emptyMap<String, Any>()

        android.util.Log.d("ProfileEdit", "Updating profile with imageUrl: ${currentProfileData?.basicInfo?.imageUrl}")

        FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(userId)
            .child("survey")
            .updateChildren(updates)
            .addOnSuccessListener {
                currentProfileData = profileData
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                showSuccessMessage()
                fetchProfileData()
                finish()
            }
            .addOnFailureListener { error ->
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                showErrorMessage(error.message ?: "Failed to update profile")
            }
    }

    private fun getCurrentProfileDataFromUI(): ProfileData {
        val medications = medicationAdapter.getMedications().map {
            ProfileData.CurrentHealthInfo.Medication(
                name = it.name,
                dosage = it.dosage,
                frequency = it.frequency
            )
        }
        return ProfileData(
            basicInfo = ProfileData.BasicInfo(
                firstName = binding.firstNameInput.text.toString(),
                lastName = binding.lastNameInput.text.toString(),
                age = binding.ageInput.text.toString().toIntOrNull(),
                gender = binding.genderDropdown.text.toString(),
                contactNumber = binding.phoneInput.text.toString(),
                height = binding.heightSlider.value,
                weight = binding.weightSlider.value,
                imageUrl = currentProfileData?.basicInfo?.imageUrl ?: "" // Preserve imageUrl here too
            ),
            bloodGroup = ProfileData.BloodGroupInfo(
                bloodGroup = getSelectedBloodGroup(),
                rhFactor = getSelectedRhFactor()
            ),
            currentHealth = ProfileData.CurrentHealthInfo(
                symptoms = getSelectedSymptoms(),
                medications = medications,
                lifestyleHabits = getSelectedLifestyleHabits(),
                sleepDuration = binding.sleepDurationSlider.value,
                exercise = getSelectedExercise(),

            ),
            medicalHistory = ProfileData.MedicalHistory(
                chronicConditions = getSelectedChronicConditions(),
                familyMedicalHistory = familyMedicalHistory.map {
                    ProfileData.MedicalHistory.FamilyCondition(
                        relation = it["relation"] ?: "",
                        condition = it["condition"] ?: "",
                        details = it["details"] ?: ""
                    )
                }
            ),
            emergencyContact = ProfileData.EmergencyContact(
                name = binding.emergencyNameInput.text.toString(),
                relation = binding.emergencyRelationInput.text.toString(),
                phone = binding.emergencyPhoneInput.text.toString(),
               
            )
        )
    }

    private fun getSelectedBloodGroup(): String {
        val checkedChipId = binding.bloodGroupChipGroup.checkedChipId
        return when (checkedChipId) {
            R.id.chipAPositive, R.id.chipANegative -> "A"
            R.id.chipBPositive, R.id.chipBNegative -> "B"
            R.id.chipABPositive, R.id.chipABNegative -> "AB"
            R.id.chipOPositive, R.id.chipONegative -> "O"
            else -> ""
        }
    }

    private fun getSelectedRhFactor(): String {
        val checkedChipId = binding.bloodGroupChipGroup.checkedChipId
        return when (checkedChipId) {
            R.id.chipAPositive, R.id.chipBPositive, R.id.chipABPositive, R.id.chipOPositive -> "Positive"
            R.id.chipANegative, R.id.chipBNegative, R.id.chipABNegative, R.id.chipONegative -> "Negative"
            else -> ""
        }
    }

    private fun getSelectedSymptoms(): List<String> {
        val symptoms = mutableListOf<String>()
        binding.symptomsChipGroup.checkedChipIds.forEach { id ->
            symptoms.add(findViewById<Chip>(id).text.toString())
        }
        return symptoms
    }

    private fun getSelectedLifestyleHabits(): List<String> {
        val habits = mutableListOf<String>()
        binding.lifestyleChipGroup.checkedChipIds.forEach { id ->
            habits.add(findViewById<Chip>(id).text.toString())
        }
        return habits
    }

    private fun getSelectedExercise(): String {
        return when (binding.exerciseRadioGroup.checkedRadioButtonId) {
            R.id.radioDaily -> "Daily"
            R.id.radio3Times -> "3-4 times a week"
            R.id.radioOnce -> "Once a week"
            R.id.radioRarely -> "Rarely"
            else -> ""
        }
    }

    private fun getSelectedChronicConditions(): List<String> {
        val conditions = mutableListOf<String>()
        binding.conditionsChipGroup.checkedChipIds.forEach { id ->
            conditions.add(findViewById<Chip>(id).text.toString())
        }
        binding.otherConditionsInput.text.toString().takeIf { it.isNotEmpty() }?.let { conditions.add(it) }
        return conditions
    }

    private fun generateReportImage(profileData: ProfileData): File? {
        val reportView = LayoutInflater.from(this).inflate(R.layout.layout_patient_report, null)

        // Set a larger initial height to accommodate all content
        val width = 2480 // A4 width at 300 DPI
        val initialHeight = 3508 // A4 height at 300 DPI as a starting point
        reportView.layoutParams = LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT)
        reportView.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(initialHeight, View.MeasureSpec.AT_MOST)
        )
        reportView.layout(0, 0, width, reportView.measuredHeight)

        // Log the measured height to debug
        android.util.Log.d("ProfileEdit", "Measured height: ${reportView.measuredHeight}")

        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        reportView.findViewById<TextView>(R.id.reportDate).text = getString(
            R.string.report_generated_on,
            dateFormat.format(Date())
        )

        reportView.findViewById<TextView>(R.id.basicInfoText).text = getString(
            R.string.report_basic_info,
            profileData.basicInfo?.firstName ?: "Not specified",
            profileData.basicInfo?.lastName ?: "",
            profileData.basicInfo?.age?.toString() ?: getString(R.string.na),
            profileData.basicInfo?.gender ?: "Not specified",
            profileData.basicInfo?.height ?: 0f,
            profileData.basicInfo?.weight ?: 0f,
            profileData.basicInfo?.contactNumber ?: "Not specified"
        )

        reportView.findViewById<TextView>(R.id.bloodGroupText).text = getString(
            R.string.report_blood_group,
            profileData.bloodGroup?.bloodGroup?.takeIf { it.isNotEmpty() } ?: "Not specified",
            profileData.bloodGroup?.rhFactor?.takeIf { it.isNotEmpty() } ?: "Not specified"
        )

        reportView.findViewById<TextView>(R.id.currentHealthText).text = getString(
            R.string.report_current_health,
            profileData.currentHealth?.symptoms?.joinToString(", ")?.takeIf { it.isNotEmpty() } ?: "None",
            profileData.currentHealth?.medications?.joinToString(", ") { "${it.name} (${it.dosage}, ${it.frequency})" }?.takeIf { it.isNotEmpty() } ?: "None",
            profileData.currentHealth?.lifestyleHabits?.joinToString(", ")?.takeIf { it.isNotEmpty() } ?: "None",
            profileData.currentHealth?.exercise?.takeIf { it.isNotEmpty() } ?: "Not specified",
            profileData.currentHealth?.sleepDuration ?: 0f
        )

        reportView.findViewById<TextView>(R.id.medicalHistoryText).text = getString(
            R.string.report_medical_history,
            profileData.medicalHistory?.chronicConditions?.joinToString(", ")?.takeIf { it.isNotEmpty() } ?: "None",
            profileData.medicalHistory?.familyMedicalHistory?.joinToString(", ") { "${it.relation}: ${it.condition} (${it.details})" }?.takeIf { it.isNotEmpty() } ?: "None"
        )

        reportView.findViewById<TextView>(R.id.emergencyContactText).text = getString(
            R.string.report_emergency_contact,
            profileData.emergencyContact?.name?.takeIf { it.isNotEmpty() } ?: "Not specified",
            profileData.emergencyContact?.relation?.takeIf { it.isNotEmpty() } ?: "Not specified",
            profileData.emergencyContact?.phone?.takeIf { it.isNotEmpty() } ?: "Not specified"
        )

        // Ensure the bitmap captures the full height
        val bitmap = Bitmap.createBitmap(width, reportView.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        reportView.draw(canvas)

        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "PatientReport_${System.currentTimeMillis()}.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            android.util.Log.d("ProfileEdit", "Report saved to: ${file.absolutePath}")
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("ProfileEdit", "Error saving report: ${e.message}")
            return null
        }
    }

    private fun sharePatientReport() {
        if (currentProfileData == null) {
            showErrorMessage("Profile data not loaded. Please try again.")
            android.util.Log.e("ProfileEdit", "currentProfileData is null, cannot generate report")
            return
        }

        android.util.Log.d("ProfileEdit", "Generating report with Firebase data: $currentProfileData")
        val reportFile = generateReportImage(currentProfileData!!)

        reportFile?.let { file ->
            val uri = FileProvider.getUriForFile(this, "com.corps.healthmate.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Patient Health Report")
                putExtra(Intent.EXTRA_TEXT, "Here is the patient health report generated by HealthMate.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Patient Report"))
        } ?: run {
            showErrorMessage("Failed to generate report")
        }
    }

    private fun showSuccessMessage() {
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}