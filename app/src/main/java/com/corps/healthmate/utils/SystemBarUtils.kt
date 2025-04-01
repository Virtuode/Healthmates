package com.corps.healthmate.utils

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemBarUtils {
    fun setupSystemBars(activity: Activity, lightStatusBar: Boolean = true, lightNavigationBar: Boolean = true) {
        // Make system bars transparent and extend content behind them
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        
        // Get the WindowInsetsController
        val window = activity.window
        val decorView = window.decorView
        val controller = WindowInsetsControllerCompat(window, decorView)
        
        // Set system bar appearance
        controller.isAppearanceLightStatusBars = lightStatusBar
        controller.isAppearanceLightNavigationBars = lightNavigationBar

        // Set flags for transparent navigation bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
        }
    }
} 