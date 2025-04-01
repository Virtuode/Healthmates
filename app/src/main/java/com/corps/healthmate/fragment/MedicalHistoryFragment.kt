package com.corps.healthmate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.corps.healthmate.R
import com.corps.healthmate.databinding.FragmentMedicalHistoryBinding
import com.corps.healthmate.interfaces.SurveyDataProvider
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MedicalHistoryFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentMedicalHistoryBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private var cachedChronicConditions: List<String>? = null
    private var cachedOtherConditions: String? = null
    private var cachedFamilyHistory: List<Map<String, String>>? = null

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
        applyCachedData()
    }

    private fun applyCachedData() {
        _binding?.let { binding ->
            cachedChronicConditions?.forEach { condition ->
                if (condition.startsWith("Other: ")) {
                    binding.otherConditionsInput.setText(condition.removePrefix("Other: "))
                } else {
                    binding.conditionsChipGroup.children.forEach { chip ->
                        if (chip is Chip && chip.text.toString() == condition) chip.isChecked = true
                    }
                }
            }
            cachedOtherConditions?.let { binding.otherConditionsInput.setText(it) }
            cachedFamilyHistory?.let {
                familyMedicalHistory.clear()
                familyMedicalHistory.addAll(it)
                updateFamilyHistoryDisplay()
            }
        }
    }

    private fun setupChronicConditionsChips() {
        binding.conditionsChipGroup.children.forEach { if (it is Chip) it.isCheckable = true }
    }

    private fun setupFamilyMedicalHistorySection() {
        binding.addFamilyMedicalHistoryButton.setOnClickListener {
            val relation = binding.familyRelationInput.text.toString().trim()
            val condition = binding.familyConditionInput.text.toString().trim()
            val details = binding.familyMedicalHistoryDetailsInput.text.toString().trim()

            if (relation.isNotEmpty() && condition.isNotEmpty()) {
                familyMedicalHistory.add(mapOf("relation" to relation, "condition" to condition, "details" to details))
                updateFamilyHistoryDisplay()
                clearFamilyHistoryInputs()
            }
        }
    }

    private fun updateFamilyHistoryDisplay() {
        binding.familyHistoryContainer.removeAllViews()
        familyMedicalHistory.forEach { entry ->
            val textView = LayoutInflater.from(context)
                .inflate(android.R.layout.simple_list_item_1, binding.familyHistoryContainer, false) as android.widget.TextView
            textView.text = getString(
                R.string.relation_condition_details,
                entry["relation"],
                entry["condition"],
                entry["details"]
            )

            binding.familyHistoryContainer.addView(textView)
        }
    }

    private fun clearFamilyHistoryInputs() {
        binding.familyRelationInput.text?.clear()
        binding.familyConditionInput.text?.clear()
        binding.familyMedicalHistoryDetailsInput.text?.clear()
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
        return binding.conditionsChipGroup.checkedChipIds.isNotEmpty() ||
                binding.otherConditionsInput.text.toString().trim().isNotEmpty()
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val medicalHistory = data["medicalHistory"] as? Map<*, *> ?: return
        if (_binding == null) {
            cachedChronicConditions = (medicalHistory["chronicConditions"] as? List<*>)?.mapNotNull { it?.toString() }
            cachedOtherConditions = null // Reset since it’s handled by chronic conditions
            cachedFamilyHistory = (medicalHistory["familyMedicalHistory"] as? List<*>)?.filterIsInstance<Map<String, String>>()

            return
        }

        (medicalHistory["chronicConditions"] as? List<*>)?.forEach { condition ->
            val conditionStr = condition.toString()
            if (conditionStr.startsWith("Other: ")) {
                binding.otherConditionsInput.setText(conditionStr.removePrefix("Other: "))
            } else {
                binding.conditionsChipGroup.children.forEach { chip ->
                    if (chip is Chip && chip.text.toString() == conditionStr) chip.isChecked = true
                }
            }
        }
        (medicalHistory["familyMedicalHistory"] as? List<*>)?.filterIsInstance<Map<String, String>>()?.let {
            familyMedicalHistory.clear()
            familyMedicalHistory.addAll(it)
            updateFamilyHistoryDisplay()
        }
    }

    private fun getChronicConditions(): List<String> {
        val conditions = binding.conditionsChipGroup.checkedChipIds.mapNotNull { id ->
            binding.conditionsChipGroup.findViewById<Chip>(id)?.text?.toString()
        }.toMutableList()
        binding.otherConditionsInput.text.toString().trim().let { if (it.isNotEmpty()) conditions.add("Other: $it") }
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