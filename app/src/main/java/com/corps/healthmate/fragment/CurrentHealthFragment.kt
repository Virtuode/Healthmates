package com.corps.healthmate.fragment

import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageButton
import androidx.core.view.children
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.corps.healthmate.R
import com.corps.healthmate.databinding.FragmentCurrentHealthBinding
import com.corps.healthmate.interfaces.SurveyDataProvider
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CurrentHealthFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentCurrentHealthBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

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
        applyCachedData()
    }

    private fun applyCachedData() {
        _binding?.let { binding ->
            cachedSymptoms?.forEach { symptom ->
                binding.symptomsChipGroup.children.forEach { chip ->
                    if (chip is Chip && chip.text.toString() == symptom) chip.isChecked = true
                }
            }
            cachedLifestyleHabits?.forEach { habit ->
                binding.lifestyleChipGroup.children.forEach { chip ->
                    if (chip is Chip && chip.text.toString() == habit) chip.isChecked = true
                }
            }
            cachedSleepDuration?.let { binding.sleepDurationSlider.value = it }
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
        }
    }

    private fun setupChipGroups() {
        binding.symptomsChipGroup.children.forEach { if (it is Chip) it.isCheckable = true }
        binding.lifestyleChipGroup.children.forEach { if (it is Chip) it.isCheckable = true }
        binding.symptomsChipGroup.setOnCheckedStateChangeListener { _, _ -> updateCurrentHealthData() }
        binding.lifestyleChipGroup.setOnCheckedStateChangeListener { _, _ -> updateCurrentHealthData() }
    }

    private fun setupMedicationInputs() {
        binding.addMedicationButton.setOnClickListener { addNewMedicationInput() }
        addNewMedicationInput() // Add initial input
    }

    private fun addNewMedicationInput() {
        val medicationView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_medication_input, binding.medicationsContainer, false)
        setupMedicationView(medicationView)
        binding.medicationsContainer.addView(medicationView)
        medications.add(MedicationInput(medicationView))
    }

    private fun setupMedicationView(view: View) {
        try {
            val nameInput = view.findViewById<TextInputEditText>(R.id.nameInput)
            val dosageInput = view.findViewById<TextInputEditText>(R.id.dosageInput)
            val frequencyInput = view.findViewById<AutoCompleteTextView>(R.id.frequencyInput)
            val removeButton = view.findViewById<ImageButton>(R.id.removeButton)

            val nameLayout = view.findViewById<TextInputLayout>(R.id.nameLayout)
            val dosageLayout = view.findViewById<TextInputLayout>(R.id.dosageLayout)
            val frequencyLayout = view.findViewById<TextInputLayout>(R.id.frequencyLayout)

            val frequencies = listOf("Once a day", "Twice a day", "Every 8 hours", "As needed")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, frequencies)
            frequencyInput.setAdapter(adapter)

            nameInput.addTextChangedListener { text: Editable? ->
                val medication = medications.find { it.view == view }
                medication?.name = text?.toString()?.trim() ?: ""
                nameLayout.error = if (medication?.name.isNullOrEmpty()) "Required" else null
            }

            dosageInput.addTextChangedListener { text: Editable? ->
                val medication = medications.find { it.view == view }
                medication?.dosage = text?.toString()?.trim() ?: ""
                dosageLayout.error = if (medication?.dosage.isNullOrEmpty()) "Required" else null
            }

            frequencyInput.setOnItemClickListener { _, _, position, _ ->
                val medication = medications.find { it.view == view }
                medication?.frequency = frequencies[position]
                frequencyLayout.error = if (medication?.frequency.isNullOrEmpty()) "Required" else null
            }

            removeButton.setOnClickListener {
                medications.removeAll { it.view == view }
                binding.medicationsContainer.removeView(view)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting up medication view")
            binding.medicationsContainer.removeView(view)
        }
    }

    private fun setupExerciseRadioGroup() {
        binding.exerciseRadioGroup.setOnCheckedChangeListener { _, _ -> updateCurrentHealthData() }
    }

    private fun setupSleepDurationSlider() {
        if (binding.sleepDurationSlider.value == 0f) binding.sleepDurationSlider.value = 6f
        binding.sleepDurationSlider.addOnChangeListener { _, _, _ -> updateCurrentHealthData() }
    }

    private fun setupStressLevelRadioGroup() {
        binding.stressLevelGroup.setOnCheckedChangeListener { _, _ -> updateCurrentHealthData() }
    }

    private fun updateCurrentHealthData() {
        // Implement update logic for current health data
    }

    override fun getSurveyData(): Map<String, Any?> {
        return mapOf(
            "currentHealth" to mapOf(
                "symptoms" to binding.symptomsChipGroup.checkedChipIds.mapNotNull { id ->
                    binding.symptomsChipGroup.findViewById<Chip>(id)?.text?.toString()
                },
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
        return areSymptomsSelected() && isStressLevelSelected() && isMedicationValid() && isExerciseSelected()
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val currentHealth = data["currentHealth"] as? Map<*, *> ?: return
        if (_binding == null) {
            cachedSymptoms = (currentHealth["symptoms"] as? List<*>)?.mapNotNull { it?.toString() }
            cachedLifestyleHabits = (currentHealth["lifestyleHabits"] as? List<*>)?.mapNotNull { it?.toString() }
            cachedSleepDuration = (currentHealth["sleepDuration"] as? Number)?.toFloat()
            cachedExercise = currentHealth["exercise"] as? String
            cachedStressLevel = currentHealth["stressLevel"] as? String
            return
        }

        (currentHealth["symptoms"] as? List<*>)?.forEach { symptom ->
            binding.symptomsChipGroup.children.forEach { chip ->
                if (chip is Chip && chip.text.toString() == symptom.toString()) chip.isChecked = true
            }
        }
        (currentHealth["lifestyleHabits"] as? List<*>)?.forEach { habit ->
            binding.lifestyleChipGroup.children.forEach { chip ->
                if (chip is Chip && chip.text.toString() == habit.toString()) chip.isChecked = true
            }
        }
        (currentHealth["medications"] as? List<*>)?.forEach { med ->
            (med as? Map<*, *>)?.let { medMap ->
                addNewMedicationInput()
                val lastMed = medications.last()
                lastMed.view.findViewById<TextInputEditText>(R.id.nameInput).setText(medMap["name"]?.toString())
                lastMed.view.findViewById<TextInputEditText>(R.id.dosageInput).setText(medMap["dosage"]?.toString())
                lastMed.view.findViewById<AutoCompleteTextView>(R.id.frequencyInput).setText(medMap["frequency"]?.toString())
                lastMed.name = medMap["name"]?.toString() ?: ""
                lastMed.dosage = medMap["dosage"]?.toString() ?: ""
                lastMed.frequency = medMap["frequency"]?.toString() ?: ""
            }
        }
        (currentHealth["sleepDuration"] as? Number)?.toFloat()?.let { binding.sleepDurationSlider.value = it }
        (currentHealth["exercise"] as? String)?.let { exercise ->
            when (exercise) {
                "Daily" -> binding.radioDaily.isChecked = true
                "3-4 times a week" -> binding.radio3Times.isChecked = true
                "Once a week" -> binding.radioOnce.isChecked = true
                "Rarely" -> binding.radioRarely.isChecked = true
            }
        }
        (currentHealth["stressLevel"] as? String)?.let { stress ->
            when (stress) {
                "Low" -> binding.lowStress.isChecked = true
                "Medium" -> binding.mediumStress.isChecked = true
                "High" -> binding.highStress.isChecked = true
            }
        }
    }

    private fun getLifestyleHabits(): List<String> {
        return binding.lifestyleChipGroup.checkedChipIds.mapNotNull { id ->
            binding.lifestyleChipGroup.findViewById<Chip>(id)?.text?.toString()
        }
    }

    private fun getExerciseFrequency(): String {
        return when {
            binding.radioDaily.isChecked -> "Daily"
            binding.radio3Times.isChecked -> "3-4 times a week"
            binding.radioOnce.isChecked -> "Once a week"
            binding.radioRarely.isChecked -> "Rarely"
            else -> ""
        }
    }

    private fun getSelectedStressLevel(): String {
        return when {
            binding.lowStress.isChecked -> "Low"
            binding.mediumStress.isChecked -> "Medium"
            binding.highStress.isChecked -> "High"
            else -> ""
        }
    }

    private fun areSymptomsSelected(): Boolean = binding.symptomsChipGroup.checkedChipIds.isNotEmpty()
    private fun isStressLevelSelected(): Boolean = binding.stressLevelGroup.checkedRadioButtonId != -1
    private fun isExerciseSelected(): Boolean = binding.exerciseRadioGroup.checkedRadioButtonId != -1
    private fun isMedicationValid(): Boolean {
        return medications.isEmpty() || medications.all { it.name.isNotEmpty() && it.dosage.isNotEmpty() && it.frequency.isNotEmpty() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = CurrentHealthFragment()
    }
}