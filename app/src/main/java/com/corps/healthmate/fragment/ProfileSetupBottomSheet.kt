package com.corps.healthmate.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.corps.healthmate.R
import com.corps.healthmate.activities.SurveyScreen
import com.corps.healthmate.databinding.BottomSheetProfileSetupBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProfileSetupBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetProfileSetupBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnGoToSurvey.setOnClickListener {
            startActivity(Intent(requireContext(), SurveyScreen::class.java))
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 