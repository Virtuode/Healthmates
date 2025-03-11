package com.corps.healthmate.activities


import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import com.corps.healthmate.R
import com.corps.healthmate.data.ProfileData
import com.corps.healthmate.databinding.ActivityProfileEditBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import androidx.recyclerview.widget.LinearLayoutManager
import com.corps.healthmate.adapters.FamilyHistoryAdapter
import com.google.android.material.chip.Chip
import com.corps.healthmate.adapters.MedicationAdapter
import com.corps.healthmate.utils.ViewAnimation
import com.corps.healthmate.utils.SystemBarUtils

@AndroidEntryPoint
class ProfileEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileEditBinding
    private var currentProfileData: ProfileData? = null
    private val familyMedicalHistory = mutableListOf<Map<String, String>>()
    private lateinit var familyHistoryAdapter: FamilyHistoryAdapter
    private lateinit var medicationAdapter: MedicationAdapter
    private val expandedStates = mutableMapOf<Int, Boolean>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBarUtils.setupSystemBars(this)
        binding = ActivityProfileEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupExpandableCards()
        setupGenderDropdown()
        setupSaveButton()
        setupFamilyMedicalHistorySection()
        setupFamilyHistoryRecyclerView()
        setupMedicationsRecyclerView()
        setupAddMedicationButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_back_24)
            title = "Settings"
        }
        
        // Update toolbar layout params to properly handle status bar
        val params = binding.toolbar.layoutParams
        params.height = resources.getDimensionPixelSize(R.dimen.toolbar_height) + getStatusBarHeight()
        binding.toolbar.layoutParams = params
        
        // Add status bar padding only to the top
        binding.toolbar.setPadding(
            binding.toolbar.paddingLeft,
            getStatusBarHeight(),
            binding.toolbar.paddingRight,
            binding.toolbar.paddingBottom
        )
        
        binding.toolbar.setNavigationOnClickListener { 
            super.onBackPressedDispatcher.onBackPressed() 
        }
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else {
            0
        }
    }

    private fun setupExpandableCards() {
        // Main profile card
        setupExpandableCard(
            headerView = binding.profileMainHeader,
            contentView = binding.profileMainContent,
            expandButton = binding.profileMainExpandButton,
            titleView = binding.profileMainTitle
        )

        // Basic info card
        setupExpandableCard(
            headerView = binding.basicInfoHeader,
            contentView = binding.basicInfoContent,
            expandButton = binding.basicInfoExpandButton,
            titleView = binding.basicInfoTitle
        )

        // Add Current Health card
        setupExpandableCard(
            headerView = binding.currentHealthHeader,
            contentView = binding.currentHealthContent,
            expandButton = binding.currentHealthExpandButton,
            titleView = binding.currentHealthTitle
        )

        // Medical history card
        setupExpandableCard(
            headerView = binding.medicalHistoryHeader,
            contentView = binding.medicalHistoryContent,
            expandButton = binding.medicalHistoryExpandButton,
            titleView = binding.medicalHistoryTitle
        )

        // Emergency contact card
        setupExpandableCard(
            headerView = binding.emergencyContactHeader,
            contentView = binding.emergencyContactContent,
            expandButton = binding.emergencyContactExpandButton,
            titleView = binding.emergencyContactTitle
        )

        // App Settings card
        setupExpandableCard(
            headerView = binding.appSettingsHeader,
            contentView = binding.appSettingsContent,
            expandButton = binding.appSettingsExpandButton,
            titleView = binding.appSettingsTitle
        )
    }

    private fun setupExpandableCard(
        headerView: View,
        contentView: View,
        expandButton: ImageButton,
        titleView: TextView
    ) {
        // Initialize state
        expandedStates[headerView.id] = headerView.id == R.id.profileMainHeader
        
        val toggleExpansion = {
            expandedStates[headerView.id] = !(expandedStates[headerView.id] ?: false)
            if (expandedStates[headerView.id] == true) {
                ViewAnimation.expand(contentView)
                expandButton.animate().rotation(180f).duration = 200
            } else {
                ViewAnimation.collapse(contentView)
                expandButton.animate().rotation(0f).duration = 200
            }
        }

        headerView.setOnClickListener { toggleExpansion() }
        expandButton.setOnClickListener { toggleExpansion() }
        
        // Initially collapse all sections except main profile
        if (headerView.id != R.id.profileMainHeader) {
            contentView.visibility = View.GONE
        }
    }

    private fun setupGenderDropdown() {
        val genders = arrayOf("Male", "Female", "Other")
        val adapter = ArrayAdapter(this, R.layout.dropdown_item, genders)
        binding.genderDropdown.setAdapter(adapter)
    }



    private fun setupSaveButton() {
        binding.saveButton.setOnClickListener {
            if (validateInputs()) {
                updateProfile()
            }
        }
    }

    private fun updateProfile() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.saveButton.isEnabled = false

        // Create a map to store only the changed fields
        val updates = mutableMapOf<String, Any>()

        // Check and add basic info changes
        binding.firstNameInput.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["basicInfo/firstName"] = it
        }
        binding.lastNameInput.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["basicInfo/lastName"] = it
        }
        binding.ageInput.text?.toString()?.toIntOrNull()?.let {
            updates["basicInfo/age"] = it
        }
        binding.genderDropdown.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["basicInfo/gender"] = it
        }
        binding.phoneInput.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["basicInfo/contactNumber"] = it
        }
        
        // Only update height and weight if they've been changed from default values
        if (binding.heightSlider.value != 170f) {
            updates["basicInfo/height"] = binding.heightSlider.value
        }
        if (binding.weightSlider.value != 70f) {
            updates["basicInfo/weight"] = binding.weightSlider.value
        }

        // Check and add emergency contact changes
        binding.emergencyNameInput.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["emergencyContact/name"] = it
        }
        binding.emergencyRelationInput.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["emergencyContact/relation"] = it
        }
        binding.emergencyPhoneInput.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
            updates["emergencyContact/phone"] = it
        }

        // Only update family medical history if there are entries
        if (familyMedicalHistory.isNotEmpty()) {
            val familyConditions = familyMedicalHistory.map { map ->
                ProfileData.MedicalHistory.FamilyCondition(
                    relation = map["relation"] ?: "",
                    condition = map["condition"] ?: "",
                    details = map["details"] ?: ""
                )
            }
            updates["medicalHistory/familyMedicalHistory"] = familyConditions
        }

        // Only update medications if there are any
        medicationAdapter.getMedications().takeIf { it.isNotEmpty() }?.let {
            updates["currentHealth/medications"] = it
        }

        // If no updates, show message and return
        if (updates.isEmpty()) {
            binding.progressBar.visibility = View.GONE
            binding.saveButton.isEnabled = true
            showMessage("No changes to update")
            return
        }

        // Update only the changed fields in Firebase
        FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(userId)
            .child("survey")
            .updateChildren(updates)
            .addOnSuccessListener {
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                showSuccessMessage()
                finish()
            }
            .addOnFailureListener { error ->
                binding.progressBar.visibility = View.GONE
                binding.saveButton.isEnabled = true
                showErrorMessage(error.message ?: "Failed to update profile")
            }
    }

    private fun populateFields(profileData: ProfileData) {
        // Basic Info
        profileData.basicInfo?.let { info ->
            binding.apply {
                firstNameInput.setText(info.firstName)
                lastNameInput.setText(info.lastName)
                ageInput.setText(info.age?.toString())
                genderDropdown.setText(info.gender)
                phoneInput.setText(info.contactNumber)
                heightSlider.value = info.height ?: 170f
                weightSlider.value = info.weight ?: 70f
            }
        }

        // Load family medical history
        profileData.medicalHistory?.let { history ->
            history.familyMedicalHistory.forEach { condition ->
                familyMedicalHistory.add(mapOf(
                    "relation" to (condition.relation ?: ""),
                    "condition" to (condition.condition ?: ""),
                    "details" to (condition.details ?: "")
                ))
            }
            updateFamilyHistoryDisplay()
        }

        // Emergency Contact
        profileData.emergencyContact?.let { contact ->
            binding.apply {
                emergencyNameInput.setText(contact.name)
                emergencyRelationInput.setText(contact.relation)
                emergencyPhoneInput.setText(contact.phone)
            }
        }

        // Populate medications
        profileData.currentHealth?.medications?.let { medications ->
            medicationAdapter.updateItems(medications)
        }
    }

    private fun validateInputs(): Boolean {
        // All fields are optional, so we'll just return true
        return true
    }

    private fun showSuccessMessage() {
        Snackbar.make(binding.root, "Profile updated successfully", Snackbar.LENGTH_SHORT).show()
    }

    private fun showErrorMessage(error: String) {
        Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG)
            .setAction("Retry") { binding.saveButton.performClick() }
            .show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupFamilyMedicalHistorySection() {
        binding.addFamilyMedicalHistoryButton.setOnClickListener {
            val relation = binding.familyRelationInput.text.toString().trim()
            val condition = binding.familyConditionInput.text.toString().trim()
            val details = binding.familyMedicalHistoryDetailsInput.text.toString().trim()

            if (relation.isNotEmpty() && condition.isNotEmpty()) {
                val familyCondition = mapOf(
                    "relation" to relation,
                    "condition" to condition,
                    "details" to details
                )
                familyMedicalHistory.add(familyCondition)
                clearFamilyHistoryInputs()
                updateFamilyHistoryDisplay()
            } else {
                showErrorMessage("Please enter both relation and condition")
            }
        }
    }

    private fun clearFamilyHistoryInputs() {
        binding.familyRelationInput.text?.clear()
        binding.familyConditionInput.text?.clear()
        binding.familyMedicalHistoryDetailsInput.text?.clear()
    }

    private fun setupFamilyHistoryRecyclerView() {
        familyHistoryAdapter = FamilyHistoryAdapter { position ->
            familyMedicalHistory.removeAt(position)
            updateFamilyHistoryDisplay()
        }
        
        binding.familyHistoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileEditActivity)
            adapter = familyHistoryAdapter
        }
    }

    private fun updateFamilyHistoryDisplay() {
        familyHistoryAdapter.updateItems(familyMedicalHistory)
    }

    private fun setupMedicationsRecyclerView() {
        medicationAdapter = MedicationAdapter { position ->
            // Handle medication removal
            currentProfileData?.currentHealth?.medications?.toMutableList()?.let { medications ->
                medications.removeAt(position)
                medicationAdapter.updateItems(medications)
            }
        }

        binding.medicationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ProfileEditActivity)
            adapter = medicationAdapter
        }
    }

    private fun setupAddMedicationButton() {
        binding.addMedicationButton.setOnClickListener {
            // Add empty medication to the list
            val newMedication = ProfileData.CurrentHealthInfo.Medication(
                name = "",
                dosage = "",
                frequency = ""
            )
            val currentMedications = currentProfileData?.currentHealth?.medications?.toMutableList() 
                ?: mutableListOf()
            currentMedications.add(newMedication)
            medicationAdapter.updateItems(currentMedications)
        }
    }
}



