package com.corps.healthmate.repository


import com.corps.healthmate.database.ActivityLogDao
import com.corps.healthmate.database.BadgeDao
import com.corps.healthmate.database.ChallengeDao
import com.corps.healthmate.database.UserProfileDao
import com.corps.healthmate.models.ActivityLog
import com.corps.healthmate.models.Badge
import com.corps.healthmate.models.Challenge
import com.corps.healthmate.models.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class GamificationRepository @Inject constructor(
    private val activityLogDao: ActivityLogDao,
    private val challengeDao: ChallengeDao,
    private val userProfileDao: UserProfileDao,
    private val badgeDao: BadgeDao
) {
    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _activeChallenge = MutableStateFlow<Challenge?>(null)
    val activeChallenge: StateFlow<Challenge?> = _activeChallenge.asStateFlow()

    private val _badges = MutableStateFlow<List<Badge>>(emptyList())
    val badges: StateFlow<List<Badge>> = _badges.asStateFlow()

    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val profile = userProfileDao.getProfile() ?: UserProfile().also { userProfileDao.insert(it) }
            _userProfile.value = profile
            _badges.value = badgeDao.getAllBadges()
            _activeChallenge.value = challengeDao.getActiveChallenge()
        }
    }

    suspend fun updateSteps(steps: Int) {
        withContext(Dispatchers.IO) {
            val profile = _userProfile.value ?: return@withContext
            val newDailySteps = profile.dailySteps + steps
            val newTotalSteps = profile.totalSteps + steps
            val calories = newDailySteps * 0.04
            val carbonSaved = newDailySteps * 0.00002
            val xpGain = steps / 10
            val newLevel = calculateLevel(profile.xp + xpGain)

            val updatedProfile = profile.copy(
                dailySteps = newDailySteps,
                totalSteps = newTotalSteps,
                totalCalories = profile.totalCalories + calories,
                totalCarbonSaved = profile.totalCarbonSaved + carbonSaved,
                xp = profile.xp + xpGain,
                level = newLevel
            ).also { checkStreak(it) }
            userProfileDao.update(updatedProfile)
            _userProfile.value = updatedProfile

            activityLogDao.insert(ActivityLog(steps = steps, timestamp = System.currentTimeMillis()))

            _activeChallenge.value?.let { challenge ->
                val updatedChallenge = challenge.copy(currentSteps = challenge.currentSteps + steps)
                if (updatedChallenge.currentSteps >= updatedChallenge.goalSteps && challenge.status != "completed") {
                    updatedChallenge.status = "completed"
                    awardXp(updatedChallenge.rewardXp)
                    awardBadge("Challenge Master", "Completed a challenge!")
                }
                challengeDao.update(updatedChallenge)
                _activeChallenge.value = updatedChallenge
            }

            checkBadges(newTotalSteps)
        }
    }

    suspend fun startChallenge(challenge: Challenge) {
        withContext(Dispatchers.IO) {
            challengeDao.insert(challenge)
            _activeChallenge.value = challenge
        }
    }

    suspend fun setActiveChallenge(challenge: Challenge) {
        withContext(Dispatchers.IO) {
            _activeChallenge.value = challenge
        }
    }

    suspend fun getActiveChallenge(): Challenge? {
        return withContext(Dispatchers.IO) {
            challengeDao.getActiveChallenge()
        }
    }

    suspend fun resetDailySteps() {
        withContext(Dispatchers.IO) {
            val profile = _userProfile.value ?: return@withContext
            val updatedProfile = profile.copy(dailySteps = 0)
            userProfileDao.update(updatedProfile)
            _userProfile.value = updatedProfile
        }
    }

    private fun calculateLevel(xp: Int): Int = (xp / 1000) + 1

    private suspend fun checkStreak(profile: UserProfile) {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        val lastDay = profile.lastStreakDate
        val diff = (today - lastDay) / (1000 * 60 * 60 * 24)
        val updatedProfile = profile.copy(
            streak = when {
                diff == 1L -> profile.streak + 1
                diff > 1L -> 0
                else -> profile.streak
            },
            lastStreakDate = today
        )
        userProfileDao.update(updatedProfile)
        _userProfile.value = updatedProfile
    }

    private suspend fun awardXp(amount: Int) {
        val profile = _userProfile.value ?: return
        val updatedProfile = profile.copy(
            xp = profile.xp + amount,
            level = calculateLevel(profile.xp + amount)
        )
        userProfileDao.update(updatedProfile)
        _userProfile.value = updatedProfile
    }

    private suspend fun awardBadge(name: String, description: String) {
        val badge = Badge(name = name, description = description, earnedDate = System.currentTimeMillis())
        badgeDao.insert(badge)
        _badges.value = badgeDao.getAllBadges()
    }

    private suspend fun checkBadges(totalSteps: Long) {
        if (totalSteps >= 10000 && _badges.value.none { it.name == "10K Steps" }) {
            awardBadge("10K Steps", "Walked 10,000 steps!")
        }
    }
}