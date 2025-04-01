package com.corps.healthmate.activities

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.adapters.AppointmentHistoryAdapter
import com.corps.healthmate.models.Appointment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import timber.log.Timber

class AppointmentHistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var completedCount: TextView
    private lateinit var missedCount: TextView
    private lateinit var totalCount: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_history)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        historyRecyclerView = findViewById(R.id.history_recycler_view)
        completedCount = findViewById(R.id.completed_count)
        missedCount = findViewById(R.id.missed_count)
        totalCount = findViewById(R.id.total_count)

        // Setup RecyclerView
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        loadAppointmentHistory()
    }

    private fun loadAppointmentHistory() {
        val patientId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val appointmentsRef = FirebaseDatabase.getInstance().reference
            .child("patients")
            .child(patientId)
            .child("appointments")

        appointmentsRef.get().addOnSuccessListener { snapshot ->
            val appointments = snapshot.children.mapNotNull { child ->
                child.getValue(Appointment::class.java)?.also { appt ->
                    val idField = appt.javaClass.getDeclaredField("id").apply { isAccessible = true }
                    idField.set(appt, child.key ?: "")
                }
            }

            // Calculate stats
            val completed = appointments.count { it.status == Appointment.STATUS_COMPLETED }
            val missed = appointments.count { it.status == Appointment.STATUS_MISSED }
            val total = appointments.size

            // Update UI
            completedCount.text = "Completed: $completed"
            missedCount.text = "Missed: $missed"
            totalCount.text = "Total: $total"

            // Setup adapter
            historyRecyclerView.adapter = AppointmentHistoryAdapter(appointments)
        }.addOnFailureListener { e ->
            Timber.e(e, "Failed to load appointment history")
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}