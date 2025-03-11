package com.corps.healthmate.fragment

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.ui.ActivityGoalsBottomSheet
import com.corps.healthmate.viewmodel.StepTrackingViewModel
import com.corps.healthmate.viewmodel.StatsViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import dagger.hilt.android.AndroidEntryPoint
import com.corps.healthmate.data.model.ActivityLog
import com.corps.healthmate.ui.adapters.ActivityLogAdapter
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class GamificationFragment : Fragment(), SensorEventListener, ActivityGoalsBottomSheet.OnGoalSetListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var lastStepCount = 0
    private var isFirstStep = true

    private val stepTrackingViewModel: StepTrackingViewModel by viewModels()


    private lateinit var motivationalMessage: TextView
    private lateinit var startActivityButton: Button
    private lateinit var setGoalButton: FloatingActionButton
    private lateinit var monthlyStatsTextView: TextView
    private lateinit var mainCircularProgress: CircularProgressIndicator
    private lateinit var carbonSavingsProgress: CircularProgressIndicator
    private lateinit var caloriesBurnedProgress: CircularProgressIndicator
    private lateinit var activityCard: View
    private lateinit var timerTextView: TextView
    private lateinit var stepsTextView: TextView
    private lateinit var activityProgressBar: LinearProgressIndicator

    private var timer: CountDownTimer? = null
    private var elapsedTime: Long = 0
    private val activityLogs = mutableListOf<ActivityLog>()
    private lateinit var activityLogAdapter: ActivityLogAdapter

    // Define a monthly step goal (e.g., 10,000 steps)
    private val monthlyStepGoal = 10000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gamification, container, false)

        motivationalMessage = view.findViewById(R.id.motivationalMessage)
        startActivityButton = view.findViewById(R.id.startActivityButton)
        setGoalButton = view.findViewById(R.id.createActivityFab)
        monthlyStatsTextView = view.findViewById(R.id.stepsThisMonth)
        mainCircularProgress = view.findViewById(R.id.mainCircularProgress)
        carbonSavingsProgress = view.findViewById(R.id.carbonSavingsProgress)
        caloriesBurnedProgress = view.findViewById(R.id.caloriesBurnedProgress)
        activityCard = view.findViewById(R.id.activityCard)
        timerTextView = view.findViewById(R.id.timerTextView)
        stepsTextView = view.findViewById(R.id.stepsTextView)
        activityProgressBar = view.findViewById(R.id.activityProgressBar)

        val activityLogRecyclerView: RecyclerView = view.findViewById(R.id.recentActivitiesList)
        loadActivityLogs() // Load persisted logs
        activityLogAdapter = ActivityLogAdapter(activityLogs)
        activityLogRecyclerView.layoutManager = LinearLayoutManager(context)
        activityLogRecyclerView.adapter = activityLogAdapter

        startActivityButton.setOnClickListener {
            if (stepTrackingViewModel.goalSteps.value == 0) {
                showSetGoalDialog()
            } else {
                if (stepTrackingViewModel.isActivityActive.value) {
                    stopActivity()
                } else {
                    startActivity()
                }
            }
        }

        setGoalButton.setOnClickListener {
            showSetGoalDialog()
        }


        viewLifecycleOwner.lifecycleScope.launch {
            stepTrackingViewModel.totalMonthlySteps.collect { totalSteps ->
                monthlyStatsTextView.text = "$totalSteps steps"
                updateProgressIndicators(totalSteps)
            }
        }

        updateUI()
        return view
    }

    private fun startActivity() {
        stepTrackingViewModel.startActivity()
        activityCard.visibility = View.VISIBLE
        startActivityButton.text = "Stop Activity"
        startActivityButton.setBackgroundColor(resources.getColor(R.color.colorAccent))
        startTimer()
    }

    private fun stopActivity() {
        val completedActivity = stepTrackingViewModel.stopActivity()
        activityCard.visibility = View.GONE
        startActivityButton.text = "Start Activity"
        startActivityButton.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        stopTimer()
        completedActivity?.let { logActivity(it.steps) }
    }

    private fun logActivity(steps: Int) {
        val logEntry = ActivityLog(steps, System.currentTimeMillis())
        activityLogs.add(logEntry)
        activityLogAdapter.notifyItemInserted(activityLogs.size - 1)
        saveActivityLogs() // Save after adding new log
        Timber.d("Activity logged: Steps=$steps, Total logs=${activityLogs.size}")
    }

    private fun showSetGoalDialog() {
        val dialog = ActivityGoalsBottomSheet()
        dialog.setOnGoalSetListener(this)
        dialog.show(childFragmentManager, "SetGoalDialog")
    }

    private fun updateUI() {
        motivationalMessage.text = stepTrackingViewModel.motivationalMessage.value
        val currentSteps = stepTrackingViewModel.currentSteps.value
        val goalSteps = stepTrackingViewModel.goalSteps.value
        stepsTextView.text = "Steps: $currentSteps/$goalSteps"
        activityProgressBar.progress = if (goalSteps > 0) (currentSteps * 100) / goalSteps else 0
    }

    private fun updateProgressIndicators(totalMonthlySteps: Int) {
        val mainProgress = if (monthlyStepGoal > 0) (totalMonthlySteps * 100) / monthlyStepGoal else 0
        mainCircularProgress.setProgress(mainProgress, true)

        val carbonSaved = stepTrackingViewModel.currentCarbonSaved.value
        val maxCarbon = 1.0 // Adjusted for smaller scale
        val carbonProgress = if (maxCarbon > 0) ((carbonSaved / maxCarbon) * 100).toInt().coerceIn(0, 100) else 0
        carbonSavingsProgress.setProgress(carbonProgress, true)

        val caloriesBurned = stepTrackingViewModel.currentCalories.value
        val maxCalories = 100.0 // Adjusted for smaller scale
        val caloriesProgress = if (maxCalories > 0) ((caloriesBurned / maxCalories) * 100).toInt().coerceIn(0, 100) else 0
        caloriesBurnedProgress.setProgress(caloriesProgress, true)
    }

    override fun onResume() {
        super.onResume()
        initializeStepSensor()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    private fun initializeStepSensor() {
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                if (isFirstStep) {
                    lastStepCount = it.values[0].toInt()
                    isFirstStep = false
                    return
                }
                val steps = it.values[0].toInt() - lastStepCount
                if (steps > 0) {
                    stepTrackingViewModel.updateSteps(steps)
                    updateUI()
                    updateProgressIndicators(stepTrackingViewModel.totalMonthlySteps.value)
                    updateCaloriesProgress(stepTrackingViewModel.currentCalories.value)
                    updateCarbonProgress(stepTrackingViewModel.currentCarbonSaved.value)
                }
                lastStepCount = it.values[0].toInt()
            }
        }
    }

    private fun updateCaloriesProgress(totalCalories: Double) {
        val maxCalories = 1000.0
        val caloriesProgress = if (maxCalories > 0) ((totalCalories / maxCalories) * 100).toInt().coerceIn(0, 100) else 0
        caloriesBurnedProgress.setProgress(caloriesProgress, true)
        println("Calories Progress: $caloriesProgress")
    }

    private fun updateCarbonProgress(totalCarbon: Double) {
        val maxCarbon = 100.0
        val carbonProgress = if (maxCarbon > 0) ((totalCarbon / maxCarbon) * 100).toInt().coerceIn(0, 100) else 0
        carbonSavingsProgress.setProgress(carbonProgress, true)
        println("Carbon Progress: $carbonProgress")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onGoalSet(steps: Int, calories: Double) {
        stepTrackingViewModel.updateGoal(steps)
        stepsTextView.text = "Steps: ${stepTrackingViewModel.currentSteps.value}/$steps"
        updateUI()
    }

    private fun startTimer() {
        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime += 1000
                updateTimerText()
            }
            override fun onFinish() {}
        }.start()
    }

    private fun stopTimer() {
        timer?.cancel()
        timer = null
        elapsedTime = 0
    }

    private fun updateTimerText() {
        val seconds = (elapsedTime / 1000) % 60
        val minutes = (elapsedTime / 1000) / 60
        timerTextView.text = String.format("%02d:%02d", minutes, seconds)
    }

    private fun saveActivityLogs() {
        val sharedPrefs = requireContext().getSharedPreferences("healthmate_prefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        val gson = Gson()
        val json = gson.toJson(activityLogs)
        editor.putString("activity_logs", json)
        editor.apply()
        Timber.d("Activity logs saved: $json")
    }

    private fun loadActivityLogs() {
        val sharedPrefs = requireContext().getSharedPreferences("healthmate_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPrefs.getString("activity_logs", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<ActivityLog>>() {}.type
            val loadedLogs: MutableList<ActivityLog> = gson.fromJson(json, type)
            activityLogs.clear()
            activityLogs.addAll(loadedLogs)
            Timber.d("Activity logs loaded: $json")
        }
    }

    // Optional: Method to clear logs (call this when user wants to clear data)
    fun clearActivityLogs() {
        activityLogs.clear()
        activityLogAdapter.notifyDataSetChanged()
        saveActivityLogs() // Persist the cleared state
        Timber.d("Activity logs cleared")
    }



    companion object {
        private const val MOVEMENT_THRESHOLD = 30000L
    }
}