package com.corps.healthmate.fragment

import com.corps.healthmate.interfaces.SurveyDataProvider
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.corps.healthmate.databinding.FragmentEmergencyContactBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EmergencyContactFragment : Fragment(), SurveyDataProvider {
    private var _binding: FragmentEmergencyContactBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null. Is the view visible?")

    // Cache for contact details
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
        setupInputValidation()
        applyCachedData()
    }

    private fun applyCachedData() {
        if (_binding == null) return

        cachedContactData?.let { data ->
            binding.contactNameInput.setText(data["name"])
            binding.relationshipInput.setText(data["relation"])
            binding.phoneInput.setText(data["phone"])
            binding.emailInput.setText(data["email"])
        }

        // Clear cached data after applying
        cachedContactData = null
    }

    private fun setupInputValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Remove updateEmergencyContactData() as it's no longer needed
            }
        }

        binding.contactNameInput.addTextChangedListener(textWatcher)
        binding.relationshipInput.addTextChangedListener(textWatcher)
        binding.phoneInput.addTextChangedListener(textWatcher)
        binding.emailInput.addTextChangedListener(textWatcher)
    }

    override fun getSurveyData(): Map<String, Any?> {
        return mapOf(
            "emergencyContact" to mapOf(
                "name" to getContactName(),
                "relation" to getContactRelation(),
                "phone" to getContactPhone(),
                "email" to getContactEmail()
            )
        )
    }

    override fun isDataValid(): Boolean {
        return isContactNameValid() && 
               isContactRelationValid() && 
               isContactPhoneValid()
    }

    override fun loadExistingData(data: Map<String, Any?>) {
        val emergencyContact = data["emergencyContact"] as? Map<*, *> ?: return

        if (_binding == null) {
            // Cache the data for later
            cachedContactData = mapOf(
                "name" to (emergencyContact["name"] as? String ?: ""),
                "relation" to (emergencyContact["relation"] as? String ?: ""),
                "phone" to (emergencyContact["phone"] as? String ?: ""),
                "email" to (emergencyContact["email"] as? String ?: "")
            )
            return
        }

        // If binding is available, apply the data directly
        binding.contactNameInput.setText(emergencyContact["name"] as? String)
        binding.relationshipInput.setText(emergencyContact["relation"] as? String)
        binding.phoneInput.setText(emergencyContact["phone"] as? String)
        binding.emailInput.setText(emergencyContact["email"] as? String)
    }

    // Public methods for validation in SurveyScreen
    fun getContactName(): String =
        binding.contactNameInput.text.toString().trim()

    fun getContactRelation(): String =
        binding.relationshipInput.text.toString().trim()

    fun getContactPhone(): String =
        binding.phoneInput.text.toString().trim()

    fun getContactEmail(): String =
        binding.emailInput.text.toString().trim()

    fun isContactNameValid(): Boolean =
        getContactName().isNotEmpty()

    fun isContactRelationValid(): Boolean =
        getContactRelation().isNotEmpty()

    fun isContactPhoneValid(): Boolean =
        getContactPhone().isNotEmpty()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = EmergencyContactFragment()
    }
}