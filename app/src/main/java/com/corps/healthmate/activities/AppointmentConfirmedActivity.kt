package com.corps.healthmate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.corps.healthmate.R
import com.corps.healthmate.models.Chat
import com.corps.healthmate.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class AppointmentConfirmedActivity : AppCompatActivity() {
    private lateinit var tvPaymentId: TextView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvAppointmentTime: TextView
    private lateinit var tvAmount: TextView
    private lateinit var btnDone: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appointment_confirmed)

        setupViews()
        displayAppointmentDetails()
        createChatInstance()
        setupDoneButton()
    }

    private fun setupViews() {
        tvPaymentId = findViewById(R.id.tvPaymentId)
        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvAppointmentTime = findViewById(R.id.tvAppointmentTime)
        tvAmount = findViewById(R.id.tvAmount)
        btnDone = findViewById(R.id.btnDone)
    }

    private fun displayAppointmentDetails() {
        intent.extras?.let { bundle ->
            tvPaymentId.text = "Payment ID: ${bundle.getString("paymentId", "N/A")}"
            tvDoctorName.text = "Doctor: ${bundle.getString("doctorName", "N/A")}"
            tvAppointmentTime.text = "Appointment Time: ${bundle.getString("appointmentTime", "N/A")}"
            tvAmount.text = "Amount Paid: ₹${bundle.getInt("amount", 0)}"
        }
    }

    private fun createChatInstance() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Log.e("AppointmentConfirmed", "User not authenticated")
            return
        }

        val doctorId = intent.getStringExtra("doctorId")
        val appointmentTime = intent.getStringExtra("appointmentTime")
        val doctorName = intent.getStringExtra("doctorName")
        val doctorImageUrl = intent.getStringExtra("doctorImageUrl")
        val paymentId = intent.getStringExtra("paymentId")

        if (doctorId == null || appointmentTime == null || doctorName == null) {
            Log.e("AppointmentConfirmed", "Missing required chat data: doctorId=$doctorId, time=$appointmentTime, name=$doctorName")
            return
        }

        val chatRef = FirebaseDatabase.getInstance().reference.child("chats").push()
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val parts = appointmentTime.split(" ", limit = 2)
        val standardizedTime = "$currentDate ${parts[1]}" // e.g., "2025-03-01 14:00"

        val chat = Chat(
            id = chatRef.key ?: "",
            doctorId = doctorId,
            patientId = currentUser.uid,
            appointmentId = paymentId ?: "",
            appointmentTime = standardizedTime,
            doctorName = doctorName,
            doctorImageUrl = doctorImageUrl ?: "",
            lastMessage = "Appointment confirmed",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isActive = true
        )

        // Save the chat instance
        chatRef.setValue(chat)
            .addOnSuccessListener {
                Log.d("AppointmentConfirmed", "Chat instance created successfully")
                // Create initial system message
                val messageRef = FirebaseDatabase.getInstance().reference
                    .child("messages")
                    .child(chat.id)
                    .push()

                val systemMessage = Message(
                    id = messageRef.key ?: "",
                    senderId = "system",
                    message = "Appointment confirmed for ${parts[0]} at ${parts[1]}",
                    timestamp = System.currentTimeMillis(),
                    type = "system",
                    status = "delivered"
                )

                messageRef.setValue(systemMessage)
                    .addOnFailureListener { e ->
                        Log.e("AppointmentConfirmed", "Failed to create system message", e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("AppointmentConfirmed", "Failed to create chat instance", e)
            }
    }

    private fun setupDoneButton() {
        btnDone.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}