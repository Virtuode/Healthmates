package com.corps.healthmate.activities

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.corps.healthmate.R
import com.corps.healthmate.fragment.NoInternetFragment
import com.corps.healthmate.utils.BottomNavIndicator
import com.corps.healthmate.utils.MedicineDataLoader
import com.corps.healthmate.utils.NetworkUtils
import com.corps.healthmate.utils.SystemBarUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import timber.log.Timber
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import androidx.navigation.fragment.NavHostFragment

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var navController: NavController
    private lateinit var bottomNavIndicator: BottomNavIndicator
    private var isNetworkAvailable = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SystemBarUtils.setupSystemBars(this)
        checkAndRequestPermissions()
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize NavController safely
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? NavHostFragment
        navController = navHostFragment?.navController
            ?: throw IllegalStateException("NavHostFragment not found for fragment_container")

        setupBottomNavigation()
        setupNetworkMonitoring()
        setupEmergencyButton()

        // Set default selection
        findViewById<LinearLayout>(R.id.nav_home).performClick()

        handleFragmentNavigation(intent)
    }

    private fun setupBottomNavigation() {
        val indicator = findViewById<View>(R.id.bottom_nav_indicator)
        val bottomNavContainer = findViewById<LinearLayout>(R.id.bottom_navigation_container)
        bottomNavIndicator = BottomNavIndicator(indicator, bottomNavContainer)

        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            navController.popBackStack(R.id.aiAssistFragment, true)
            navController.navigate(R.id.aiAssistFragment)
            updateNavigationColors(R.id.nav_home)
            bottomNavIndicator.updateIndicatorPosition(0)
        }

        findViewById<LinearLayout>(R.id.nav_inbox).setOnClickListener {
            navController.popBackStack(R.id.aiAssistFragment, true)
            navController.navigate(R.id.doctorsFragment)
            updateNavigationColors(R.id.nav_inbox)
            bottomNavIndicator.updateIndicatorPosition(1)
        }

        findViewById<LinearLayout>(R.id.nav_calendar).setOnClickListener {
            navController.popBackStack(R.id.aiAssistFragment, true)
            navController.navigate(R.id.chatFragmentPatient)
            updateNavigationColors(R.id.nav_calendar)
            bottomNavIndicator.updateIndicatorPosition(3)
        }

        findViewById<LinearLayout>(R.id.nav_profile).setOnClickListener {
            navController.popBackStack(R.id.aiAssistFragment, true)
            navController.navigate(R.id.gamificationFragment)
            updateNavigationColors(R.id.nav_profile)
            bottomNavIndicator.updateIndicatorPosition(4)
        }

        findViewById<LinearLayout>(R.id.nav_call).setOnClickListener {
            handleEmergencyCall()
        }
    }

    private fun updateNavigationColors(selectedId: Int) {
        val navItems = listOf(R.id.nav_home, R.id.nav_inbox, R.id.nav_calendar, R.id.nav_profile)
        navItems.forEach { itemId ->
            val item = findViewById<LinearLayout>(itemId)
            val icon = item.getChildAt(0) as ImageView
            val text = item.getChildAt(1) as TextView
            if (itemId == selectedId) {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.tab_selected))
                text.setTextColor(ContextCompat.getColor(this, R.color.tab_selected))
            } else {
                icon.setColorFilter(ContextCompat.getColor(this, R.color.tab_unselected))
                text.setTextColor(ContextCompat.getColor(this, R.color.tab_unselected))
            }
        }
    }

    private fun handleEmergencyCall() {
        Toast.makeText(this, "Emergency Call Initiated", Toast.LENGTH_SHORT).show()
        val vibrator = getVibrator(this)
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        startActivity(Intent(this, EmergencyHandlerActivity::class.java))
    }

    private fun setupNetworkMonitoring() {
        lifecycleScope.launch {
            NetworkUtils.getNetworkStatus(applicationContext)
                .distinctUntilChanged()
                .collect { isConnected ->
                    isNetworkAvailable = isConnected
                    if (isConnected) {
                        navController.navigate(navController.currentDestination?.id ?: R.id.aiAssistFragment)
                    } else {
                        showNoInternetFragment()
                    }
                }
        }
    }

    private fun setupEmergencyButton() {
        val fabEmergency = findViewById<ImageView>(R.id.fabEmergency)
        fabEmergency.setOnClickListener {
            val vibrator = getVibrator(this)
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
            startActivity(Intent(this, EmergencyHandlerActivity::class.java))
        }
    }

    private fun showNoInternetFragment() {
        if (isFinishing || isDestroyed) return

        val noInternetFragment = NoInternetFragment.newInstance {
            if (isNetworkAvailable) {
                navController.popBackStack(R.id.aiAssistFragment, true)
                navController.navigate(navController.currentDestination?.id ?: R.id.aiAssistFragment)
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, noInternetFragment)
            .commit()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.POST_NOTIFICATIONS) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "Notification permission granted.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getVibrator(context: Context): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private fun forceMedicineReload() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                MedicineDataLoader(applicationContext).forceReloadMedicines()
            } catch (e: Exception) {
                Timber.e(e, "Failed to reload medicines")
            }
        }
    }

    private fun handleFragmentNavigation(intent: Intent?) {
        intent?.getStringExtra("openFragment")?.let { fragmentToOpen ->
            if (fragmentToOpen == "home") {
                navController.popBackStack(R.id.aiAssistFragment, true)
                navController.navigate(R.id.aiAssistFragment)
                updateNavigationColors(R.id.nav_home)
                bottomNavIndicator.updateIndicatorPosition(0)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleFragmentNavigation(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}