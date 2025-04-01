package com.corps.healthmate.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.corps.healthmate.databinding.FragmentEmergencyContactBinding
import com.corps.healthmate.interfaces.SurveyDataProvider
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyContactFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentEmergencyContactBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")

    private var cachedContactData: Map<String, String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmergencyContactBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyCachedData()
    }

    private fun applyCachedData() {
        _binding?.let { binding ->
            cachedContactData?.let { data ->
                binding.contactNameInput.setText(data["name"])
                binding.relationshipInput.setText(data["relation"])
                binding.phoneInput.setText(data["phone"])
                binding.emailInput.setText(data["email"])
            }
        }
    }

    override fun getSurveyData(): Map<String, Any?> {
        return mapOf(
            "emergencyContact" to mapOf(
                "name" to binding.contactNameInput.text.toString().trim(),
                "relation" to binding.relationshipInput.text.toString().trim(),
                "phone" to binding.phoneInput.text.toString().trim(),
                "email" to binding.emailInput.text.toString().trim()
            )
        )
    }

    override fun isDataValid(): Boolean {
        return binding.contactNameInput.text.toString().trim().isNotEmpty() &&
                binding.relationshipInput.text.toString().trim().isNotEmpty() &&
                binding.phoneInput.text.toString().trim().isNotEmpty()
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val emergencyContact = data["emergencyContact"] as? Map<*, *> ?: return
        if (_binding == null) {
            cachedContactData = mapOf(
                "name" to (emergencyContact["name"] as? String ?: ""),
                "relation" to (emergencyContact["relation"] as? String ?: ""),
                "phone" to (emergencyContact["phone"] as? String ?: ""),
                "email" to (emergencyContact["email"] as? String ?: "")
            )
            return
        }
        binding.contactNameInput.setText(emergencyContact["name"] as? String)
        binding.relationshipInput.setText(emergencyContact["relation"] as? String)
        binding.phoneInput.setText(emergencyContact["phone"] as? String)
        binding.emailInput.setText(emergencyContact["email"] as? String)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EmergencyContactFragment()
    }
}