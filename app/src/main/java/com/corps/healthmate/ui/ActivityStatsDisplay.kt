package com.corps.healthmate.ui

import android.os.Bundle
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.corps.healthmate.R
import com.corps.healthmate.viewmodel.StatsViewModel

class ActivityStatsDisplay : AppCompatActivity() {

    private lateinit var viewModel: StatsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_step_tracking)

        viewModel = ViewModelProvider(this).get(StatsViewModel::class.java)

        val currentStepsTextView = findViewById<TextView>(R.id.currentSteps)
        val monthlyStatsTextView = findViewById<TextView>(R.id.monthlyStats)
        val recentActivitiesListView = findViewById<ListView>(R.id.recentActivitiesList)

        // Update UI with data from ViewModel
        monthlyStatsTextView.text = "Monthly Stats: ${viewModel.monthlyStats.value}"
        currentStepsTextView.text = "Current Stats: ${viewModel.currentStats.value}"
        // Set adapter for recent activities
    }
} 