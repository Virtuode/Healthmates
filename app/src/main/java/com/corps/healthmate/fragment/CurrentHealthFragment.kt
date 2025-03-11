package com.corps.healthmate.fragment

import com.corps.healthmate.interfaces.SurveyDataProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.corps.healthmate.databinding.FragmentCurrentHealthBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import android.widget.ImageButton
import android.widget.AutoCompleteTextView

import com.corps.healthmate.R
import com.google.android.material.textfield.TextInputEditText
import timber.log.Timber

@AndroidEntryPoint
class CurrentHealthFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentCurrentHealthBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Is the view visible?")

    // Cache for selected values
    private var cachedSymptoms: List<String>? = null
    private var cachedLifestyleHabits: List<String>? = null
    private var cachedSleepDuration: Float? = null
    private var cachedExercise: String? = null
    private var cachedStressLevel: String? = null

    private val medications = mutableListOf<MedicationInput>()
    
    data class MedicationInput(
        val view: View,
        var name: String = "",
        var dosage: String = "",
        var frequency: String = ""
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrentHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChipGroups()
        setupMedicationInputs()
        setupExerciseRadioGroup()
        setupSleepDurationSlider()
        setupStressLevelRadioGroup()
        
        // Apply cached data if it exists
        applyCachedData()
    }

    private fun applyCachedData() {
        if (_binding == null) return

        cachedSymptoms?.forEach { symptom ->
            binding.symptomsChipGroup.children.forEach { chip ->
                if (chip is Chip && chip.text.toString() == symptom) {
                    chip.isChecked = true
                }
            }
        }

        cachedLifestyleHabits?.forEach { habit ->
            binding.lifestyleChipGroup.children.forEach { chip ->
                if (chip is Chip && chip.text.toString() == habit) {
                    chip.isChecked = true
                }
            }
        }

        cachedSleepDuration?.let {
            binding.sleepDurationSlider.value = it
        }

        cachedExercise?.let { exercise ->
            when (exercise) {
                "Daily" -> binding.radioDaily.isChecked = true
                "3-4 times a week" -> binding.radio3Times.isChecked = true
                "Once a week" -> binding.radioOnce.isChecked = true
                "Rarely" -> binding.radioRarely.isChecked = true
            }
        }

        cachedStressLevel?.let { stress ->
            when (stress) {
                "Low" -> binding.lowStress.isChecked = true
                "Medium" -> binding.mediumStress.isChecked = true
                "High" -> binding.highStress.isChecked = true
            }
        }

        // Clear cached data after applying
        clearCachedData()
    }

    private fun clearCachedData() {
        cachedSymptoms = null
        cachedLifestyleHabits = null
        cachedSleepDuration = null
        cachedExercise = null
        cachedStressLevel = null
    }

    private fun setupChipGroups() {
        // Make sure all chips are checkable
        binding.symptomsChipGroup.children.forEach { chip ->
            if (chip is Chip) {
                chip.isCheckable = true
            }
        }
        
        binding.lifestyleChipGroup.children.forEach { chip ->
            if (chip is Chip) {
                chip.isCheckable = true
            }
        }

        binding.symptomsChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            updateCurrentHealthData()
        }
        
        binding.lifestyleChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            updateCurrentHealthData()
        }
    }

    private fun setupMedicationInputs() {
        binding.addMedicationButton.setOnClickListener {
            addNewMedicationInput()
        }
        
        // Add first medication input by default
        addNewMedicationInput()
    }

    private fun addNewMedicationInput() {
        val medicationView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_medication_input, binding.medicationsContainer, false)
        
        // Setup the new medication input
        setupMedicationView(medicationView)
        
        // Add to container
        binding.medicationsContainer.addView(medicationView)
        
        // Add to tracking list
        medications.add(MedicationInput(medicationView))
    }

    private fun setupMedicationView(view: View) {
        try {
            val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
                ?: throw IllegalStateException("nameInput not found in medication view")
            val dosageInput = view.findViewById<TextInputEditText>(R.id.dosageInput)
                ?: throw IllegalStateException("dosageInput not found in medication view")
            val frequencyInput = view.findViewById<AutoCompleteTextView>(R.id.frequencyInput)
                ?: throw IllegalStateException("frequencyInput not found in medication view")
            val removeButton = view.findViewById<ImageButton>(R.id.removeButton)
                ?: throw IllegalStateException("removeButton not found in medication view")

            // Setup frequency dropdown
            val frequencies = listOf("Once a day", "Twice a day", "Every 8 hours", "As needed")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies)
            frequencyInput.setAdapter(adapter)

            // Setup text change listeners
            nameInput.addTextChangedListener { text ->
                medications.find { it.view == view }?.name = text?.toString() ?: ""
                updateCurrentHealthData()
            }

            dosageInput.addTextChangedListener { text ->
                medications.find { it.view == view }?.dosage = text?.toString() ?: ""
                updateCurrentHealthData()
            }

            frequencyInput.setOnItemClickListener { _, _, position, _ ->
                medications.find { it.view == view }?.frequency = frequencies[position]
                updateCurrentHealthData()
            }

            // Setup remove button
            removeButton.setOnClickListener {
                medications.removeAll { it.view == view }
                binding.medicationsContainer.removeView(view)
                updateCurrentHealthData()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up medication view")
            // Handle the error appropriately - maybe show a toast or remove the invalid view
            medications.removeAll { it.view == view }
            binding.medicationsContainer.removeView(view)
        }
    }

    private fun setupExerciseRadioGroup() {
        binding.exerciseRadioGroup.setOnCheckedChangeListener { _, _ -> updateCurrentHealthData() }
    }

    private fun setupSleepDurationSlider() {
        // Set a default value if no value is set
        if (binding.sleepDurationSlider.value == 0f) {
            binding.sleepDurationSlider.value = 6f  // Default to 6 hours
        }

        binding.sleepDurationSlider.addOnChangeListener { _, value, _ ->
            updateCurrentHealthData()
        }

        // Trigger initial update
        updateCurrentHealthData()
    }

    private fun setupStressLevelRadioGroup() {
        binding.stressLevelGroup.setOnCheckedChangeListener { _, _ ->
            updateCurrentHealthData()
        }
    }

    private fun loadExistingData() {
        // Implement loading logic for existing data
    }

    private fun updateCurrentHealthData() {
        // Implement update logic for current health data
    }

    // Public methods for validation in SurveyScreen
    fun areSymptomsSelected(): Boolean =
        binding.symptomsChipGroup.checkedChipIds.isNotEmpty()

    fun isStressLevelSelected(): Boolean =
        binding.stressLevelGroup.checkedRadioButtonId != -1

    fun getSelectedStressLevel(): String =
        when {
            binding.lowStress.isChecked -> "Low"
            binding.mediumStress.isChecked -> "Medium"
            binding.highStress.isChecked -> "High"
            else -> ""
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun getSurveyData(): Map<String, Any?> {
        val selectedSymptoms = binding.symptomsChipGroup.checkedChipIds
            .mapNotNull { id -> 
                (binding.symptomsChipGroup.findViewById<Chip>(id)).text.toString() 
            }

        return mapOf(
            "currentHealth" to mapOf(
                "symptoms" to selectedSymptoms,
                "medications" to medications.map { med ->
                    mapOf(
                        "name" to med.name,
                        "dosage" to med.dosage,
                        "frequency" to med.frequency
                    )
                },
                "lifestyleHabits" to getLifestyleHabits(),
                "sleepDuration" to binding.sleepDurationSlider.value,
                "exercise" to getExerciseFrequency(),
                "stressLevel" to getSelectedStressLevel()
            )
        )
    }

    override fun isDataValid(): Boolean {
        return areSymptomsSelected() && 
               isStressLevelSelected() &&
               isMedicationValid() &&
               isExerciseSelected()
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val currentHealth = data["currentHealth"] as? Map<*, *> ?: return
        
        // Clear existing medication inputs
        binding.medicationsContainer.removeAllViews()
        medications.clear()

        // Load medications
        (currentHealth["medications"] as? List<*>)?.forEach { med ->
            if (med is Map<*, *>) {
                addNewMedicationInput()
                val lastMed = medications.last()
                val view = lastMed.view

                view.findViewById<TextInputEditText>(R.id.nameInput)
                    .setText(med["name"]?.toString())
                view.findViewById<TextInputEditText>(R.id.dosageInput)
                    .setText(med["dosage"]?.toString())
                view.findViewById<AutoCompleteTextView>(R.id.frequencyInput)
                    .setText(med["frequency"]?.toString())

                lastMed.name = med["name"]?.toString() ?: ""
                lastMed.dosage = med["dosage"]?.toString() ?: ""
                lastMed.frequency = med["frequency"]?.toString() ?: ""
            }
        }
    }

    private fun getLifestyleHabits(): List<String> {
        return binding.lifestyleChipGroup.checkedChipIds.mapNotNull { id ->
            (binding.lifestyleChipGroup.findViewById<Chip>(id)).text.toString()
        }
    }

    private fun getExerciseFrequency(): String {
        return when {
            binding.radioDaily.isChecked -> "Daily"
            binding.radio3Times.isChecked -> "3-4 times a week"
            binding.radioOnce.isChecked -> "Once a week"
            binding.radioRarely.isChecked -> "Rarely"
            else -> "Not Selected"
        }
    }

    private fun isMedicationValid(): Boolean {
        // If no medications added, return true (no medication is valid)
        if (medications.isEmpty()) return true
        
        // Check if all added medications have complete information
        return medications.all { med ->
            med.name.isNotEmpty() && 
            med.dosage.isNotEmpty() && 
            med.frequency.isNotEmpty()
        }
    }

    private fun isExerciseSelected(): Boolean {
        return binding.exerciseRadioGroup.checkedRadioButtonId != -1
    }

    companion object {
        fun newInstance() = CurrentHealthFragment()
    }
}