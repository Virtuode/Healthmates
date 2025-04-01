package com.corps.healthmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.corps.healthmate.models.Challenge
import com.corps.healthmate.repository.GamificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GamificationViewModel @Inject constructor(
    private val repository: GamificationRepository
) : ViewModel() {

    val userLevel = repository.userProfile.map { it?.level ?: 1 }
    val xp = repository.userProfile.map { it?.xp ?: 0 }
    val dailySteps = repository.userProfile.map { it?.dailySteps ?: 0 }
    val totalSteps = repository.userProfile.map { it?.totalSteps ?: 0 }
    val caloriesBurned = repository.userProfile.map { it?.totalCalories ?: 0.0 }
    val carbonSaved = repository.userProfile.map { it?.totalCarbonSaved ?: 0.0 }
    val streak = repository.userProfile.map { it?.streak ?: 0 }
    val activeChallenge = repository.activeChallenge
    val badges = repository.badges

    val dailyGoal = 10000

    init {
        viewModelScope.launch {
            repository.initialize()
            val challenge = repository.getActiveChallenge()
            if (challenge != null) {
                repository.setActiveChallenge(challenge)
            }
        }
    }

    fun updateSteps(steps: Int) {
        viewModelScope.launch { repository.updateSteps(steps) }
    }

    fun startChallenge(title: String, description: String, goalSteps: Int) {
        viewModelScope.launch {
            repository.startChallenge(Challenge(title = title, description = description, goalSteps = goalSteps))
        }
    }

    fun getLevelTitle(level: Int): String = when (level) {
        1 -> "Rookie"
        2 -> "Adventurer"
        3 -> "Explorer"
        else -> "Legend"
    }

    fun getNextLevelXP(): Int = (repository.userProfile.value?.level ?: 1) * 1000
}