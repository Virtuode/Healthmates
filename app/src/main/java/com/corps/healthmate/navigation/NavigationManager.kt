package com.corps.healthmate.navigation

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.corps.healthmate.activities.*
import com.google.firebase.auth.FirebaseAuth


object NavigationManager {
    private const val PREFS_NAME = "HealthmatePrefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboardingCompleted"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun handleSplashScreenNavigation(activity: AppCompatActivity) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val prefs = getPrefs(activity)
        
        // Check if onboarding is completed
        val isOnboardingCompleted = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

        when {
            !isOnboardingCompleted -> {
                navigateToOnboarding(activity)
            }
            currentUser != null -> {
                // User is logged in, go directly to main activity
                checkUserStatus(activity)
            }
            else -> {
                navigateToWelcomeScreen(activity)
            }
        }
    }

    fun handleLoginSuccess(activity: AppCompatActivity) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Mark onboarding as completed for logged-in users
            getPrefs(activity).edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
            checkUserStatus(activity)
        } else {
            navigateToWelcomeScreen(activity)
        }
    }

    fun handleRegistrationSuccess(activity: AppCompatActivity) {
        navigateToSurvey(activity)
    }

    private fun navigateToOnboarding(activity: AppCompatActivity) {
        val intent = Intent(activity, OnBoardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }



    private fun checkUserStatus(activity: AppCompatActivity) {
        navigateToMain(activity)
    }


    private fun navigateToWelcomeScreen(activity: AppCompatActivity) {
        val intent = Intent(activity, WelcomeScreenActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    private fun navigateToSurvey(activity: AppCompatActivity) {
        val intent = Intent(activity, SurveyScreen::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    private fun navigateToMain(activity: AppCompatActivity) {
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity.startActivity(intent)
        activity.finish()
    }
}
