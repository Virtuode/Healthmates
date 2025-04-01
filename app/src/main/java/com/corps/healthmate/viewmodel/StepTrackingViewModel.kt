package com.corps.healthmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corps.healthmate.models.ActivityStats
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import java.util.*

@HiltViewModel
class StepTrackingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _currentSteps = MutableStateFlow(0)
    val currentSteps: StateFlow<Int> = _currentSteps
    private val _goalSteps = MutableStateFlow(3000)
    private val _elapsedTime = MutableStateFlow(0L)
    private val _isActivityActive = MutableStateFlow(false)
    private var timerJob: Job? = null
    private var activityStartTime: Long = 0

    data class WalkingActivity(
        val date: Long = System.currentTimeMillis(),
        val duration: Long = 0,
        val steps: Int = 0,
        val calories: Double = 0.0,
        val carbonSaved: Double = 0.0,
        val isCompleted: Boolean = false
    )

    private val _completedActivities = MutableStateFlow<List<WalkingActivity>>(emptyList())

    private val _monthlyStats = MutableStateFlow<List<ActivityStats>>(emptyList())
    val monthlyStats: StateFlow<List<ActivityStats>> = _monthlyStats

    private val _recentActivities = MutableStateFlow<List<ActivityStats>>(emptyList())
    val recentActivities: StateFlow<List<ActivityStats>> = _recentActivities

    private val _currentCalories = MutableStateFlow(0.0)

    private val _totalMonthlySteps = MutableStateFlow(0)

    init {
        loadSavedActivities()
        loadActivities()
        updateMonthlyStats() // Initial calculation
    }

    private fun loadSavedActivities() {
        viewModelScope.launch {
            val activities = getStoredActivities()
            _completedActivities.value = activities
            updateMonthlyStats() // Update stats after loading
        }
    }

    fun startActivity() {
        _isActivityActive.value = true
        activityStartTime = System.currentTimeMillis()
        startTimer()
    }


    private fun startTimer() {
        timerJob?.cancel()
        _elapsedTime.value = 0

        timerJob = viewModelScope.launch {
            while (isActive && _isActivityActive.value) {
                delay(1000)
                _elapsedTime.value += 1000
            }
        }
    }


    private fun loadActivities() {
        viewModelScope.launch {
            try {
                val prefs = context.getSharedPreferences("activities", Context.MODE_PRIVATE)
                val recentJson = prefs.getString("recent_activities", null)
                val completedJson = prefs.getString("completed_activities", null)

                if (recentJson != null) {
                    val type = object : TypeToken<List<ActivityStats>>() {}.type
                    _recentActivities.value = Gson().fromJson(recentJson, type)
                }
                if (completedJson != null) {
                    val type = object : TypeToken<List<WalkingActivity>>() {}.type
                    _completedActivities.value = Gson().fromJson(completedJson, type)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load activities")
            }
        }
    }

    private fun getStoredActivities(): List<WalkingActivity> {
        return try {
            val prefs = context.getSharedPreferences("activities", Context.MODE_PRIVATE)
            val json = prefs.getString("completed_activities", null)
            if (json != null) {
                val type = object : TypeToken<List<WalkingActivity>>() {}.type
                Gson().fromJson(json, type)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }





    fun updateGoal(newGoal: Int) {
        _goalSteps.value = newGoal
    }

    fun updateCalories(newCalories: Double) {
        _currentCalories.value = newCalories
    }


    private fun updateMonthlyStats() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            val monthlyActivities = _completedActivities.value.filter {
                calendar.timeInMillis = it.date
                calendar.get(Calendar.MONTH) == currentMonth &&
                        calendar.get(Calendar.YEAR) == currentYear
            }

            _monthlyStats.value = monthlyActivities.map { activity ->
                ActivityStats.calculateStats(
                    steps = activity.steps,
                    duration = activity.duration,
                    isCompleted = activity.isCompleted
                )
            }

            // Calculate total steps for the month
            val totalSteps = monthlyActivities.sumOf { it.steps }
            _totalMonthlySteps.value = totalSteps
        }
    }
}