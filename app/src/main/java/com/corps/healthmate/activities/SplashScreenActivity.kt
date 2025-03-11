package com.corps.healthmate.activities

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.corps.healthmate.R
import com.corps.healthmate.navigation.NavigationManager

class SplashScreenActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        // Show splash screen for 2 seconds then navigate
        Handler(Looper.getMainLooper()).postDelayed({
            NavigationManager.handleSplashScreenNavigation(this)
        }, 2000)
    }
}
