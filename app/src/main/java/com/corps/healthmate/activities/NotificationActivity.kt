package com.corps.healthmate.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.corps.healthmate.R
import com.corps.healthmate.adapters.NotificationAdapter
import com.corps.healthmate.databinding.ActivityNotificationBinding
import androidx.lifecycle.lifecycleScope
import com.corps.healthmate.models.DoctorSummary
import com.corps.healthmate.models.Notification
import com.corps.healthmate.repository.AppointmentRepository
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private val repository = AppointmentRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.notificationList.layoutManager = LinearLayoutManager(this)
        fetchNotifications()

        val appointmentId = intent.getStringExtra("appointmentId")
        if (appointmentId != null) {
            lifecycleScope.launch {
                repository.getAllNotifications { notifications ->
                    val notification = notifications.find { it.appointmentId == appointmentId }
                    notification?.let { markNotificationAsRead(it.id) }
                }
            }
        }
    }

    private fun fetchNotifications() {
        repository.getAllNotifications { notifications ->
            val sortedNotifications = notifications.sortedByDescending { it.timestamp }
            binding.notificationList.adapter = NotificationAdapter(
                sortedNotifications,
                onMarkAsRead = { notificationId ->
                    lifecycleScope.launch {
                        repository.markNotificationAsRead(notificationId)
                        fetchNotifications()
                    }
                },
                onBookAgain = { appointmentId, notificationId ->
                    lifecycleScope.launch {
                        val appointment = repository.getAllAppointments("").find { it.id == appointmentId }
                        if (appointment != null) {
                            val doctorSnapshot = FirebaseDatabase.getInstance().reference
                                .child("doctors")
                                .child(appointment.doctorId)
                                .get()
                                .await()
                            val doctor = doctorSnapshot.getValue(DoctorSummary::class.java)
                            if (doctor != null) {
                                val doctorName = doctor.name ?: "Unknown"
                                val intent = Intent(this@NotificationActivity, DoctorDetailActivity::class.java).apply {
                                    putExtra("doctor", doctor)
                                    putExtra("originalAppointmentId", appointmentId) // Optional for tracking
                                }
                                startActivity(intent)
                                // Update notification to indicate rebooking started
                                repository.insertNotification(
                                    Notification(
                                        id = notificationId,
                                        appointmentId = appointmentId,
                                        type = "rebook_initiated",
                                        message = "Rebooking started for your missed appointment with Dr. $doctorName.",
                                        timestamp = System.currentTimeMillis(),
                                        read = false
                                    )
                                )
                            }
                        }
                    }
                }
            )
            binding.notificationList.visibility = if (notifications.isNotEmpty()) View.VISIBLE else View.GONE
            if (notifications.isEmpty()) {
                Timber.d("No notifications found")
            }
        }
    }

    private fun markNotificationAsRead(notificationId: String) {
        lifecycleScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }
}