package com.corps.healthmate.ui

import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.corps.healthmate.R
import androidx.lifecycle.ViewModelProvider
import com.corps.healthmate.viewmodel.StepTrackingViewModel

class StepTrackingScreen : AppCompatActivity() {

    private lateinit var viewModel: StepTrackingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_tracking)
        viewModel = ViewModelProvider(this).get(StepTrackingViewModel::class.java)

        val currentStepsTextView = findViewById<TextView>(R.id.currentSteps)
        val monthlyStatsTextView = findViewById<TextView>(R.id.monthlyStats)
        val recentActivitiesTextView = findViewById<TextView>(R.id.recentActivities)

        // Update UI with data from ViewModel
        currentStepsTextView.text = "Current Steps: ${viewModel.currentSteps.value}"
        monthlyStatsTextView.text = "Monthly Stats: ${viewModel.monthlyStats.value}"
        recentActivitiesTextView.text = "Recent Activities: ${viewModel.recentActivities.value}"
    }
} 