package com.corps.healthmate.fragment

import com.corps.healthmate.interfaces.SurveyDataProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.corps.healthmate.databinding.FragmentMedicalHistoryBinding
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MedicalHistoryFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentMedicalHistoryBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Is the view visible?")

    // Cache for selected values
    private var cachedChronicConditions: List<String>? = null
    private var cachedOtherConditions: String? = null
    private var cachedFamilyHistory: List<Map<String, String>>? = null

    // Store family medical history using a simpler structure
    private val familyMedicalHistory = mutableListOf<Map<String, String>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicalHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChronicConditionsChips()
        setupFamilyMedicalHistorySection()
        setupDataChangeListeners()
        applyCachedData()
    }

    private fun applyCachedData() {
        if (_binding == null) return

        // Apply chronic conditions
        cachedChronicConditions?.forEach { condition ->
            if (condition.startsWith("Other: ")) {
                binding.otherConditionsInput.setText(condition.removePrefix("Other: "))
            } else {
                binding.conditionsChipGroup.children.forEach { chip ->
                    if (chip is Chip && chip.text.toString() == condition) {
                        chip.isChecked = true
                    }
                }
            }
        }

        // Apply other conditions
        cachedOtherConditions?.let {
            binding.otherConditionsInput.setText(it)
        }

        // Apply family history
        cachedFamilyHistory?.let {
            familyMedicalHistory.clear()
            familyMedicalHistory.addAll(it)
            updateFamilyHistoryDisplay()
        }

        // Clear cached data after applying
        clearCachedData()
    }

    private fun clearCachedData() {
        cachedChronicConditions = null
        cachedOtherConditions = null
        cachedFamilyHistory = null
    }

    private fun setupChronicConditionsChips() {
        // Make sure all chips are checkable
        binding.conditionsChipGroup.children.forEach { chip ->
            if (chip is Chip) {
                chip.isCheckable = true
            }
        }

        binding.conditionsChipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            // Handle chip selection changes if needed
        }
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
                updateFamilyHistoryDisplay()
                clearFamilyHistoryInputs()
            }
        }
    }

    private fun updateFamilyHistoryDisplay() {
        // Update UI to show added family history
        // Implement according to your UI requirements
    }

    private fun clearFamilyHistoryInputs() {
        binding.familyRelationInput.text?.clear()
        binding.familyConditionInput.text?.clear()
        binding.familyMedicalHistoryDetailsInput.text?.clear()
    }

    private fun setupDataChangeListeners() {
        binding.otherConditionsInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun getSurveyData(): Map<String, Any?> {
        return mapOf(
            "medicalHistory" to mapOf(
                "chronicConditions" to getChronicConditions(),
                "familyMedicalHistory" to familyMedicalHistory
            )
        )
    }

    override fun isDataValid(): Boolean {
        // At least one chronic condition should be selected or other conditions filled
        return binding.conditionsChipGroup.checkedChipIds.isNotEmpty() ||
               binding.otherConditionsInput.text?.toString()?.isNotEmpty() == true
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val medicalHistory = data["medicalHistory"] as? Map<*, *> ?: return

        if (_binding == null) {
            // Cache the data for later
            cachedChronicConditions = (medicalHistory["chronicConditions"] as? List<*>)?.mapNotNull { it?.toString() }
            
            // Cache family history
            @Suppress("UNCHECKED_CAST")
            cachedFamilyHistory = medicalHistory["familyMedicalHistory"] as? List<Map<String, String>>
            return
        }

        // Load chronic conditions
        (medicalHistory["chronicConditions"] as? List<*>)?.forEach { condition ->
            val conditionStr = condition.toString()
            if (conditionStr.startsWith("Other: ")) {
                binding.otherConditionsInput.setText(conditionStr.removePrefix("Other: "))
            } else {
                binding.conditionsChipGroup.children.forEach { chip ->
                    if (chip is Chip && chip.text.toString() == conditionStr) {
                        chip.isChecked = true
                    }
                }
            }
        }

        // Load family medical history
        @Suppress("UNCHECKED_CAST")
        val familyHistory = medicalHistory["familyMedicalHistory"] as? List<Map<String, String>>
        familyHistory?.let {
            familyMedicalHistory.clear()
            familyMedicalHistory.addAll(it)
            updateFamilyHistoryDisplay()
        }
    }

    private fun getChronicConditions(): List<String> {
        val conditions = mutableListOf<String>()
        
        // Get selected chips
        binding.conditionsChipGroup.children.forEach { chip ->
            if (chip is Chip && chip.isChecked) {
                conditions.add(chip.text.toString())
            }
        }

        // Add other conditions
        binding.otherConditionsInput.text?.toString()?.trim()?.let { 
            if (it.isNotEmpty()) conditions.add("Other: $it") 
        }

        return conditions
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = MedicalHistoryFragment()
    }
}
