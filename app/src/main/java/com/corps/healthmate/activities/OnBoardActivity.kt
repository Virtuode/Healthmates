package com.corps.healthmate.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.corps.healthmate.R
import com.corps.healthmate.adapters.OnBoardPagerAdapter
import com.corps.healthmate.fragment.OnBoardFragment2
import com.corps.healthmate.fragment.OnBoardFragment3
import com.corps.healthmate.fragment.OnboardFragment1

import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnBoardActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var skipTextView: TextView
    private lateinit var getStartedButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_on_board)

        // Apply system window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize views
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        skipTextView = findViewById(R.id.skipTextView)
        getStartedButton = findViewById(R.id.getStartedButton)

        // Set up fragments
        val fragments = listOf<Fragment>(
            OnboardFragment1(),
            OnBoardFragment2(),
            OnBoardFragment3()
        )

        // Set up adapter
        val adapter = OnBoardPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        // Set up TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Handle page changes
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateNavigationButtons(position)
            }
        })

        // Set click listeners
        getStartedButton.setOnClickListener {
            if (viewPager.currentItem < fragments.size - 1) {
                viewPager.currentItem = viewPager.currentItem + 1
            } else {
                finishOnboarding()
            }
        }

        skipTextView.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun updateNavigationButtons(position: Int) {
        val isLastPage = position == 2
        skipTextView.visibility = if (isLastPage) View.GONE else View.VISIBLE
        getStartedButton.text = if (isLastPage) getString(R.string.get_started) else getString(R.string.next)
    }

    private fun finishOnboarding() {
        // Save that onboarding is completed
        getSharedPreferences("HealthmatePrefs", MODE_PRIVATE)
            .edit()
            .putBoolean("onboardingCompleted", true)
            .apply()

        // Navigate to WelcomeScreen instead of LoginActivity
        startActivity(Intent(this, WelcomeScreenActivity::class.java))
        finish()
    }
}
