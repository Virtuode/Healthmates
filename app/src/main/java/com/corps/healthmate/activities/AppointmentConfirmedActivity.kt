package com.corps.healthmate.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.corps.healthmate.R
import com.corps.healthmate.models.Chat
import com.corps.healthmate.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class AppointmentConfirmedActivity : AppCompatActivity() {
    private lateinit var tvPaymentId: TextView
    private lateinit var tvDoctorName: TextView
    private lateinit var tvAppointmentTime: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvConsultationType: TextView
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
        tvConsultationType = findViewById(R.id.tvConsultationType)
        btnDone = findViewById(R.id.btnDone)
    }

    private fun displayAppointmentDetails() {
        intent.extras?.let { bundle ->
            tvPaymentId.text = getString(R.string.payment_id, bundle.getString("paymentId", "N/A"))
            tvDoctorName.text = getString(R.string.doctor_name, bundle.getString("doctorName", "N/A"))
            tvAppointmentTime.text = getString(R.string.appointment_time, bundle.getString("appointmentTime", "N/A"))
            tvAmount.text = getString(R.string.amount_paid, bundle.getInt("amount", 0))
            tvConsultationType.text = getString(R.string.consultation_type, bundle.getString("consultationType", "N/A"))

        }
    }

    private fun createChatInstance() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            return
        }

        val doctorId = intent.getStringExtra("doctorId")
        val appointmentTime = intent.getStringExtra("appointmentTime")
        val doctorName = intent.getStringExtra("doctorName")
        val doctorImageUrl = intent.getStringExtra("doctorImageUrl")
        val paymentId = intent.getStringExtra("paymentId")
        val consultationType = intent.getStringExtra("consultationType")

        if (doctorId == null || appointmentTime == null || doctorName == null) {
            return
        }

        val chatRef = FirebaseDatabase.getInstance().reference.child("chats").push()
        val parts = appointmentTime.split(" ", limit = 2)
        val standardizedTime = "${parts[0]} ${parts[1]}"

        val chat = Chat(
            id = chatRef.key ?: "",
            doctorId = doctorId,
            patientId = currentUser.uid,
            appointmentId = paymentId ?: "",
            appointmentTime = standardizedTime,
            doctorName = doctorName,
            doctorImageUrl = doctorImageUrl ?: "",
            lastMessage = "Appointment confirmed for $consultationType",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0,
            isActive = true,
            status = "confirmed"
        )

        chatRef.setValue(chat).addOnSuccessListener {
            val messageRef = FirebaseDatabase.getInstance().reference.child("messages").child(chat.id).push()
            val systemMessage = Message(
                id = messageRef.key ?: "",
                senderId = "system",
                message = "Appointment confirmed for ${parts[0]} at ${parts[1]}",
                timestamp = System.currentTimeMillis(),
                type = "system",
                status = "delivered"
            )
            messageRef.setValue(systemMessage).addOnFailureListener {
            }
        }.addOnFailureListener {
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