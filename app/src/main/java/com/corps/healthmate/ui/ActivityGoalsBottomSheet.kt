package com.corps.healthmate.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.corps.healthmate.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.corps.healthmate.viewmodel.StepTrackingViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.fragment.app.viewModels

@AndroidEntryPoint
class ActivityGoalsBottomSheet : BottomSheetDialogFragment() {
    private lateinit var stepsInput: EditText
    private lateinit var caloriesInput: EditText
    private lateinit var startActivityButton: Button
    private var listener: OnGoalSetListener? = null

    private val stepTrackingViewModel: StepTrackingViewModel by viewModels()

    interface OnGoalSetListener {
        fun onGoalSet(steps: Int, calories: Double)
    }

    // Set the listener
    fun setOnGoalSetListener(listener: OnGoalSetListener) {
        this.listener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_activity_goals, container, false)
        stepsInput = view.findViewById(R.id.stepsInput)
        caloriesInput = view.findViewById(R.id.caloriesInput)
        startActivityButton = view.findViewById(R.id.startActivityButton)

        // Optionally set the button text programmatically
        startActivityButton.text = "Set Goals"

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startActivityButton.setOnClickListener {
            val steps = stepsInput.text.toString().toIntOrNull()
            val calories = caloriesInput.text.toString().toDoubleOrNull()
            if (steps != null && calories != null) {
                stepTrackingViewModel.updateGoal(steps)
                stepTrackingViewModel.updateCalories(calories)
                listener?.onGoalSet(steps, calories) // Notify the listener
                dismiss()
            }
        }
    }
} 