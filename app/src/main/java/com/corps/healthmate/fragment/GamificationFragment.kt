package com.corps.healthmate.fragment

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.adapters.BadgeAdapter
import com.corps.healthmate.databinding.FragmentGamificationBinding
import com.corps.healthmate.viewmodel.GamificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@AndroidEntryPoint
class GamificationFragment : Fragment(), SensorEventListener {
    private val viewModel: GamificationViewModel by viewModels()
    private var _binding: FragmentGamificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var previousStepCount = -1f

    private lateinit var badgeRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGamificationBinding.inflate(inflater, container, false)
        sensorManager = requireContext().getSystemService(SensorManager::class.java)
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        badgeRecyclerView = binding.badgeRecyclerView
        badgeRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        badgeRecyclerView.adapter = BadgeAdapter()

        binding.actionFab.setOnClickListener {
            viewModel.startChallenge("Night Runner", "Walk 5000 steps at night", 5000)
        }

        setupObservers()
        return binding.root
    }

    private fun setupObservers() {
        val decimalFormat = DecimalFormat("#.#")

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.dailySteps.collect { steps ->
                binding.stepsProgress.text = getString(R.string.steps_progress, steps, viewModel.dailyGoal)
                binding.mainCircularProgress.setProgress(
                    ((steps.toFloat() / viewModel.dailyGoal) * 100).toInt(), true
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.totalSteps.collect { total ->
                binding.totalStepsText.text = getString(R.string.total_steps, total)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.userLevel.collect { level ->
                binding.userLevel.text = getString(R.string.user_level, level, viewModel.getLevelTitle(level))
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.xp.collect { xp ->
                val nextLevelXP = viewModel.getNextLevelXP()
                binding.xpProgress.text = getString(R.string.xp_progress, xp, nextLevelXP)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.carbonSaved.collect { carbon ->
                binding.carbonText.text = getString(R.string.carbon_saved, decimalFormat.format(carbon).toFloat())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.caloriesBurned.collect { calories ->
                binding.caloriesText.text = getString(R.string.calories_burned, decimalFormat.format(calories).toFloat())
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.streak.collect { streak ->
                binding.streakText.text = getString(R.string.streak, streak)
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeChallenge.collect { challenge ->
                if (challenge != null) {
                    binding.challengeCard.visibility = View.VISIBLE
                    binding.challengeTitle.text = challenge.title
                    binding.challengeDesc.text = challenge.description
                    binding.challengeProgress.setProgress(challenge.progress, true)
                } else {
                    binding.challengeCard.visibility = View.VISIBLE
                    binding.challengeTitle.text = getString(R.string.no_active_challenge)
                    binding.challengeDesc.text = getString(R.string.join_challenge)

                    binding.challengeProgress.setProgress(0, false)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.badges.collect { badges ->
                (badgeRecyclerView.adapter as BadgeAdapter).submitList(badges)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_STEP_COUNTER) {
                val currentSteps = it.values[0]
                if (previousStepCount >= 0) {
                    val stepsTaken = (currentSteps - previousStepCount).toInt()
                    if (stepsTaken > 0) {
                        viewModel.updateSteps(stepsTaken)
                    }
                }
                previousStepCount = currentSteps
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}