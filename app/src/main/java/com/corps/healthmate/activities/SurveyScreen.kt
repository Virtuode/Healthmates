package com.corps.healthmate.activities

import com.corps.healthmate.interfaces.SurveyDataProvider
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.corps.healthmate.R
import com.corps.healthmate.databinding.ActivitySurveyScreenBinding
import com.corps.healthmate.fragment.BasicInfoFragment
import com.corps.healthmate.fragment.CurrentHealthFragment
import com.corps.healthmate.fragment.EmergencyContactFragment
import com.corps.healthmate.fragment.FragmentBloodGroup
import com.corps.healthmate.fragment.MedicalHistoryFragment
import com.corps.healthmate.data.ProfileData
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseException
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import com.corps.healthmate.utils.FirebaseReferenceManager

@AndroidEntryPoint
class SurveyScreen : AppCompatActivity() {
    private lateinit var binding: ActivitySurveyScreenBinding
    private lateinit var progressBar: ProgressBar
    private val surveyData = mutableMapOf<String, Any?>()
    private var isDataSaving = false

    // Survey fragments in order with explicit Fragment type
    private val surveyFragments: List<Fragment> = listOf(
        BasicInfoFragment.newInstance(),
        FragmentBloodGroup.newInstance(),
        CurrentHealthFragment.newInstance(),
        MedicalHistoryFragment.newInstance(),
        EmergencyContactFragment.newInstance()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySurveyScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressBar = binding.progressIndicator

        setupViewPager()
        setupNavigation()
        observePageChanges()
        setupBackPressHandler()
        loadExistingData()
    }

    private fun setupViewPager() {
        binding.viewPager.isUserInputEnabled = false // Disable swipe
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = surveyFragments.size

            override fun createFragment(position: Int): Fragment = surveyFragments[position]
        }
    }

    private fun setupNavigation() {
        binding.previousButton.setOnClickListener {
            navigateToPreviousPage()
        }

        binding.nextButton.setOnClickListener {
            navigateToNextPage()
        }

        binding.skipButton.setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun navigateToPreviousPage() {
        val currentItem = binding.viewPager.currentItem
        if (currentItem > 0) {
            binding.viewPager.currentItem = currentItem - 1
            updateProgress(currentItem - 1)
        }
    }

    private fun navigateToNextPage() {
        val currentItem = binding.viewPager.currentItem
        val totalItems = binding.viewPager.adapter?.itemCount ?: 0

        if (currentItem < totalItems - 1) {
            if (validateCurrentPage(currentItem)) {
                binding.viewPager.currentItem = currentItem + 1
                updateProgress(currentItem + 1)
            }
        } else {
            // Final submission
            if (validateCurrentPage(currentItem)) {
                submitSurvey()
            }
        }
    }

    private fun submitSurvey() {
        val profileData = collectAllSurveyData()
        saveSurveyDataToFirebase(profileData)
    }

    private fun collectAllSurveyData(): ProfileData {
        val basicInfoFragment = surveyFragments[0] as BasicInfoFragment
        val bloodGroupFragment = surveyFragments[1] as FragmentBloodGroup
        val currentHealthFragment = surveyFragments[2] as CurrentHealthFragment
        val medicalHistoryFragment = surveyFragments[3] as MedicalHistoryFragment
        val emergencyContactFragment = surveyFragments[4] as EmergencyContactFragment

        val basicInfoData = basicInfoFragment.getSurveyData()["basicInfo"] as? Map<*, *>
        val bloodGroupData = bloodGroupFragment.getSurveyData()["bloodGroup"] as? Map<*, *>
        val currentHealthData = currentHealthFragment.getSurveyData()["currentHealth"] as? Map<*, *>
        val medicalHistoryData = medicalHistoryFragment.getSurveyData()["medicalHistory"] as? Map<*, *>
        val emergencyContactData = emergencyContactFragment.getSurveyData()["emergencyContact"] as? Map<*, *>

        return ProfileData(
            basicInfo = basicInfoData?.let {
                ProfileData.BasicInfo(
                    firstName = it["firstName"] as? String ?: "",
                    middleName = it["middleName"] as? String ?: "",
                    lastName = it["lastName"] as? String ?: "",
                    age = it["age"] as? Int,
                    gender = it["gender"] as? String ?: "",
                    contactNumber = it["contactNumber"] as? String ?: "",
                    height = it["height"] as? Float,
                    weight = it["weight"] as? Float,
                    imageUrl = it["imageUrl"] as? String
                )
            },
            bloodGroup = bloodGroupData?.let {
                ProfileData.BloodGroupInfo(
                    bloodGroup = it["bloodGroup"] as? String ?: "",
                    rhFactor = it["rhFactor"] as? String ?: ""
                )
            },
            currentHealth = currentHealthData?.let {
                ProfileData.CurrentHealthInfo(
                    symptoms = (it["symptoms"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    medications = (it["medications"] as? List<*>)?.mapNotNull { med ->
                        (med as? Map<*, *>)?.let { medMap ->
                            ProfileData.CurrentHealthInfo.Medication(
                                name = medMap["name"] as? String ?: "",
                                dosage = medMap["dosage"] as? String ?: "",
                                frequency = medMap["frequency"] as? String ?: ""
                            )
                        }
                    } ?: emptyList(),
                    lifestyleHabits = (it["lifestyleHabits"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    sleepDuration = it["sleepDuration"] as? Float ?: 0f,
                    exercise = it["exercise"] as? String ?: "",
                    stressLevel = it["stressLevel"] as? String ?: ""
                )
            },
            medicalHistory = medicalHistoryData?.let {
                ProfileData.MedicalHistory(
                    chronicConditions = (it["chronicConditions"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    familyMedicalHistory = (it["familyMedicalHistory"] as? List<*>)?.mapNotNull { fam ->
                        (fam as? Map<*, *>)?.let { famMap ->
                            ProfileData.MedicalHistory.FamilyCondition(
                                relation = famMap["relation"] as? String ?: "",
                                condition = famMap["condition"] as? String ?: "",
                                details = famMap["details"] as? String ?: ""
                            )
                        }
                    } ?: emptyList()
                )
            },
            emergencyContact = emergencyContactData?.let {
                ProfileData.EmergencyContact(
                    name = it["name"] as? String ?: "",
                    relation = it["relation"] as? String ?: "",
                    phone = it["phone"] as? String ?: "",
                    email = it["email"] as? String ?: ""
                )
            }
        )
    }

    private fun saveSurveyDataToFirebase(profileData: ProfileData) {
        if (isDataSaving) {
            Timber.w("Data save already in progress")
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        isDataSaving = true
        showLoadingDialog()

        try {
            // Create a map of all updates we need to make
            val updates = mutableMapOf<String, Any>()
            
            // Add survey data to patients path
            updates["patients/$userId/survey"] = profileData
            updates["patients/$userId/surveyCompleted"] = true
            
            // Add survey completion status to users path
            updates["users/$userId/surveyCompleted"] = true
            
            // Get root reference since we're updating multiple paths
            val reference = FirebaseReferenceManager.getReference("")
            
            // Keep synced through the manager for both paths
            FirebaseReferenceManager.keepSynced("patients/$userId")
            FirebaseReferenceManager.keepSynced("users/$userId")

            // Set timeout for the operation
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                if (isDataSaving) {
                    isDataSaving = false
                    hideLoadingDialog()
                    Toast.makeText(this, "Operation timed out. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            timeoutHandler.postDelayed(timeoutRunnable, 30000) // 30 seconds timeout

            // Update all paths atomically
            reference.updateChildren(updates)
                .addOnSuccessListener {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    isDataSaving = false
                    hideLoadingDialog()
                    Toast.makeText(this, "Survey submitted successfully!", Toast.LENGTH_SHORT).show()
                    navigateToMainActivity()
                }
                .addOnFailureListener { error ->
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    isDataSaving = false
                    hideLoadingDialog()
                    
                    val errorMessage = when {
                        error is DatabaseException -> "Database error: ${error.message}"
                        error.message?.contains("permission_denied") == true -> "Permission denied. Please try again."
                        error.message?.contains("network") == true -> "Network error. Please check your connection."
                        else -> "Failed to submit survey: ${error.message}"
                    }
                    
                    Timber.e(error, "Survey submission failed")
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
                .addOnCanceledListener {
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    isDataSaving = false
                    hideLoadingDialog()
                    Toast.makeText(this, "Operation cancelled. Please try again.", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            isDataSaving = false
            hideLoadingDialog()
            Timber.e(e, "Error initializing Firebase operation")
            Toast.makeText(this, "Failed to initialize survey submission", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun validateCurrentPage(position: Int): Boolean {
        return (surveyFragments[position] as? SurveyDataProvider)?.isDataValid() ?: true
    }

    private fun observePageChanges() {
        binding.viewPager.registerOnPageChangeCallback(object :
            androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavigationButtons(position)
                updateProgress(position)
            }
        })
    }

    private fun updateNavigationButtons(position: Int) {
        // Null-safe visibility setting
        binding.previousButton?.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE

        // Null-safe text setting with fallback
        binding.nextButton?.text = when {
            position == surveyFragments.size - 1 -> getString(R.string.submit) ?: "Submit"
            else -> getString(R.string.next) ?: "Next"
        }
    }

    private fun updateProgress(position: Int) {
        // Null-safe progress updates
        val totalPages = surveyFragments.size
        val safePosition = position.coerceIn(0, totalPages - 1)
        val progress = ((safePosition + 1) * 100) / totalPages

        binding.progressText?.text = try {
            getString(R.string.progress_text, safePosition + 1, totalPages)
        } catch (e: Exception) {
            "Step ${safePosition + 1} of $totalPages"
        }

        binding.progressIndicator?.progress = progress
    }

    private fun setupBackPressHandler() {
        // Use addCallback with isEnabled to prevent multiple callbacks
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmationDialog()
            }
        }
        onBackPressedDispatcher.addCallback(this, backPressedCallback)
    }

    private fun showExitConfirmationDialog() {
        try {
            MaterialAlertDialogBuilder(this)
                .setTitle(resources.getString(R.string.exit_survey))
                .setMessage(resources.getString(R.string.exit_survey_confirmation))
                .setPositiveButton(resources.getString(R.string.yes)) { _, _ ->
                    finish()
                }
                .setNegativeButton(resources.getString(R.string.no)) { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .create()
                .show()
        } catch (e: Exception) {
            // Fallback dialog with hardcoded strings
            AlertDialog.Builder(this)
                .setTitle("Exit Survey")
                .setMessage("Are you sure you want to exit the survey?")
                .setPositiveButton("Yes") { _, _ ->
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    private fun showLoadingDialog() {
        if (!isFinishing) {
            binding.loadingOverlay.visibility = View.VISIBLE
            binding.loadingProgressBar.visibility = View.VISIBLE
            binding.nextButton.isEnabled = false
        }
    }

    private fun hideLoadingDialog() {
        if (!isFinishing) {
            binding.loadingOverlay.visibility = View.GONE
            binding.loadingProgressBar.visibility = View.GONE
            binding.nextButton.isEnabled = true
        }
    }

    private fun loadExistingData() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        showLoadingDialog()

        try {
            // Use the same consistent path as in save
            val path = "patients/$userId"
            FirebaseReferenceManager.keepSynced(path)
            
            val reference = FirebaseReferenceManager.getReference("$path/survey")

            // Set timeout
            val timeoutHandler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                hideLoadingDialog()
                Toast.makeText(this, "Loading data timed out. Please try again.", Toast.LENGTH_SHORT).show()
            }
            timeoutHandler.postDelayed(timeoutRunnable, 15000) // 15 seconds timeout

            reference.get()
                .addOnSuccessListener { snapshot ->
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    hideLoadingDialog()
                    
                    // Add type checking
                    val snapshotValue = snapshot.value
                    if (snapshotValue !is Map<*, *>) {
                        Timber.w("Survey data is not in expected format")
                        return@addOnSuccessListener
                    }
                    
                    @Suppress("UNCHECKED_CAST")
                    val data = snapshotValue as Map<String, Any?>
                    
                    surveyFragments.forEach { fragment ->
                        if (fragment is SurveyDataProvider) {
                            try {
                                fragment.loadExistingData(data)
                            } catch (e: Exception) {
                                Timber.e(e, "Error loading data into fragment")
                            }
                        }
                    }
                }
                .addOnFailureListener { error ->
                    timeoutHandler.removeCallbacks(timeoutRunnable)
                    hideLoadingDialog()
                    Timber.e(error, "Failed to load existing survey data")
                    Toast.makeText(this, "Failed to load existing data", Toast.LENGTH_SHORT).show()
                }

        } catch (e: Exception) {
            hideLoadingDialog()
            Timber.e(e, "Error initializing data load")
            Toast.makeText(this, "Failed to initialize data loading", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isDataSaving = false
        // Clear any ongoing operations
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                // Clear the sync for this specific path when the activity is destroyed
                FirebaseReferenceManager.clearSyncedReferences()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing Firebase references")
        }
    }
}