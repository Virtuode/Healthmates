package com.corps.healthmate.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.corps.healthmate.data.model.ActivityStats
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
    val goalSteps: StateFlow<Int> = _goalSteps

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    private val _motivationalMessage = MutableStateFlow<String?>(null)
    val motivationalMessage: StateFlow<String?> = _motivationalMessage

    private val _isWalking = MutableStateFlow(false)
    val isWalking: StateFlow<Boolean> = _isWalking

    private var lastStepTime = 0L
    private var stepBuffer = mutableListOf<Long>()
    private val STEP_DETECTION_WINDOW = 5000L // 5 seconds window

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private val _isActivityActive = MutableStateFlow(false)
    val isActivityActive: StateFlow<Boolean> = _isActivityActive

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
    val completedActivities: StateFlow<List<WalkingActivity>> = _completedActivities

    private val _monthlyStats = MutableStateFlow<List<ActivityStats>>(emptyList())
    val monthlyStats: StateFlow<List<ActivityStats>> = _monthlyStats

    private val _recentActivities = MutableStateFlow<List<ActivityStats>>(emptyList())
    val recentActivities: StateFlow<List<ActivityStats>> = _recentActivities

    private val _currentCalories = MutableStateFlow(0.0)
    val currentCalories: StateFlow<Double> = _currentCalories

    private val _currentCarbonSaved = MutableStateFlow(0.0)
    val currentCarbonSaved: StateFlow<Double> = _currentCarbonSaved

    // New StateFlow for total monthly steps
    private val _totalMonthlySteps = MutableStateFlow(0)
    val totalMonthlySteps: StateFlow<Int> = _totalMonthlySteps

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

    fun stopActivity(): WalkingActivity? {
        if (_isActivityActive.value) {
            _isActivityActive.value = false
            timerJob?.cancel()

            val completedActivity = WalkingActivity(
                date = System.currentTimeMillis(),
                duration = _elapsedTime.value,
                steps = _currentSteps.value,
                calories = _currentCalories.value,
                carbonSaved = _currentCarbonSaved.value,
                isCompleted = true
            )

            val activityStats = ActivityStats.calculateStats(
                steps = completedActivity.steps,
                duration = completedActivity.duration,
                isCompleted = completedActivity.isCompleted
            )

            _recentActivities.value = listOf(activityStats) + _recentActivities.value.take(9)
            _completedActivities.value = _completedActivities.value + completedActivity

            _currentSteps.value = 0
            _currentCalories.value = 0.0
            _currentCarbonSaved.value = 0.0
            _elapsedTime.value = 0

            updateMonthlyStats()
            saveActivities()

            return completedActivity
        }
        return null
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

    private fun saveActivities() {
        viewModelScope.launch {
            try {
                context.getSharedPreferences("activities", Context.MODE_PRIVATE)
                    .edit()
                    .putString("recent_activities", Gson().toJson(_recentActivities.value))
                    .putString("completed_activities", Gson().toJson(_completedActivities.value))
                    .apply()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save activities")
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

    fun formatTime(timeMillis: Long): String {
        val seconds = (timeMillis / 1000) % 60
        val minutes = (timeMillis / (1000 * 60)) % 60
        val hours = (timeMillis / (1000 * 60 * 60))
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun updateSteps(steps: Int) {
        _currentSteps.value += steps
        updateCurrentStats()
    }

    private fun updateCurrentStats() {
        val steps = _currentSteps.value
        _currentCalories.value = steps * ActivityStats.CALORIES_PER_STEP
        val distanceInKm = steps.toDouble() / ActivityStats.STEPS_PER_KM
        _currentCarbonSaved.value = distanceInKm * ActivityStats.CARBON_SAVED_PER_KM
    }

    private fun updateWalkingStatus() {
        val currentTime = System.currentTimeMillis()
        stepBuffer.add(currentTime)
        stepBuffer.removeAll { it < currentTime - STEP_DETECTION_WINDOW }
        val isCurrentlyWalking = stepBuffer.size >= 3

        if (isCurrentlyWalking != _isWalking.value) {
            _isWalking.value = isCurrentlyWalking
            if (!isCurrentlyWalking) {
                showMotivationalMessage()
            }
        }
    }



    fun updateGoal(newGoal: Int) {
        _goalSteps.value = newGoal
    }

    fun updateCalories(newCalories: Double) {
        _currentCalories.value = newCalories
    }

    fun showMotivationalMessage() {
        _motivationalMessage.value = getRandomMotivationalMessage()
        viewModelScope.launch {
            delay(3000)
            _motivationalMessage.value = null
        }
    }

    private fun getRandomMotivationalMessage(): String {
        return when ((0..4).random()) {
            0 -> "Keep moving! You're doing great!"
            1 -> "Every step counts towards your goal!"
            2 -> "Don't stop now, you're making progress!"
            3 -> "Walking is good for your health!"
            else -> "You're almost there!"
        }
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