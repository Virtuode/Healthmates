package com.corps.healthmate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.corps.healthmate.databinding.FragmentBloodGroupBinding
import com.corps.healthmate.interfaces.SurveyDataProvider
import com.google.android.material.chip.Chip

class FragmentBloodGroup : Fragment(), SurveyDataProvider {
    private var _binding: FragmentBloodGroupBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private var selectedBloodGroup: String? = null
    private var selectedRhFactor: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBloodGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBloodGroupSelection()
        setupRhFactorSelection()
    }

    private fun setupBloodGroupSelection() {
        binding.chipGroupBloodGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedBloodGroup = if (checkedIds.isNotEmpty()) {
                group.findViewById<Chip>(checkedIds[0])?.text?.toString()
            } else null
        }
    }

    private fun setupRhFactorSelection() {
        binding.chipGroupRhFactor.setOnCheckedStateChangeListener { group, checkedIds ->
            selectedRhFactor = if (checkedIds.isNotEmpty()) {
                group.findViewById<Chip>(checkedIds[0])?.text?.toString()
            } else null
        }
    }

    override fun getSurveyData(): Map<String, Any?> {
        return mapOf(
            "bloodGroup" to mapOf(
                "bloodGroup" to (selectedBloodGroup ?: ""),
                "rhFactor" to (selectedRhFactor ?: "")
            )
        )
    }

    override fun isDataValid(): Boolean {
        return !selectedBloodGroup.isNullOrEmpty() && !selectedRhFactor.isNullOrEmpty()
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val bloodGroupData = data["bloodGroup"] as? Map<*, *> ?: return
        val bloodGroup = bloodGroupData["bloodGroup"] as? String
        val rhFactor = bloodGroupData["rhFactor"] as? String

        if (_binding == null) {
            selectedBloodGroup = bloodGroup
            selectedRhFactor = rhFactor
            return
        }

        bloodGroup?.let { bg ->
            binding.chipGroupBloodGroup.findViewWithTag<Chip>(bg)?.isChecked = true
            selectedBloodGroup = bg
        }
        rhFactor?.let { rh ->
            binding.chipGroupRhFactor.findViewWithTag<Chip>(rh)?.isChecked = true
            selectedRhFactor = rh
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = FragmentBloodGroup()
    }
}