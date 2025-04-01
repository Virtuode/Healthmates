package com.corps.healthmate.utils

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.os.Build
import com.corps.healthmate.R

object ActivityTransitionUtil {
    fun startActivityWithAnimation(activity: Activity, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Use modern activity transition for Android 14+
            val options = ActivityOptions.makeCustomAnimation(
                activity,
                R.anim.slide_in_right,
                R.anim.slide_out_left
            )
            activity.startActivity(intent, options.toBundle())
        } else {
            // Fallback for older versions
            activity.startActivity(intent)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }
        }
    }
}
