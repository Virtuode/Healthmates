package com.corps.healthmate.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.corps.healthmate.R
import com.corps.healthmate.models.Appointment
import com.corps.healthmate.models.TimeSlot
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class PaymentActivity : AppCompatActivity(), PaymentResultListener {
    private lateinit var progressBar: ProgressBar
    private lateinit var summaryTextView: TextView
    private var amount: Int = 0
    private lateinit var selectedTimeSlot: TimeSlot
    private lateinit var doctorId: String
    private var doctorName: String? = null
    private var appointmentDateTime: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        progressBar = findViewById(R.id.progressBar)
        summaryTextView = findViewById(R.id.tvPaymentSummary)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Payment"

        amount = intent.getIntExtra("amount", 0)
        val timeSlot = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("selectedTimeSlot", TimeSlot::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("selectedTimeSlot")
        }
        doctorId = intent.getStringExtra("doctorId") ?: ""
        doctorName = intent.getStringExtra("doctorName")
        appointmentDateTime = intent.getStringExtra("appointmentDateTime")

        if (doctorId.isEmpty() || timeSlot == null || timeSlot.id.isEmpty() || appointmentDateTime.isNullOrEmpty()) {
            showError("Invalid appointment details. Please try again.")
            finish()
            return
        }
        selectedTimeSlot = timeSlot

        displayPaymentSummary()
        Checkout.preload(applicationContext)
        startPayment()
    }

    private fun displayPaymentSummary() {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy, HH:mm", Locale.getDefault())
        val appointmentTime = sdf.parse(appointmentDateTime!!)
        val formattedDateTime = displayFormat.format(appointmentTime)

        val summary = """
            Doctor: ${doctorName ?: "N/A"}
            Appointment: $formattedDateTime - ${selectedTimeSlot.endTime}
            Amount: ₹$amount
            
            Please complete the payment to confirm your appointment.
        """.trimIndent()
        summaryTextView.text = summary
    }

    private fun startPayment() {
        progressBar.visibility = View.VISIBLE
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_CiRpD8Y4MWq0by")
        checkout.setImage(R.drawable.applogo)

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                showError("You must be logged in to make a payment.")
                progressBar.visibility = View.GONE
                finish()
                return
            }
            Log.d("PaymentActivity", "Current user UID: ${currentUser.uid}")

            val options = JSONObject().apply {
                put("name", "HealthMate")
                put("description", "Doctor Appointment")
                put("image", "https://your-app-logo-url.png")
                put("currency", "INR")
                put("amount", amount * 100)
                put("send_sms_hash", true)
                put("prefill.email", currentUser.email ?: "")
                put("prefill.contact", currentUser.phoneNumber ?: "")
                put("method", JSONObject().apply {
                    put("upi", true)
                    put("card", true)
                    put("netbanking", true)
                    put("wallet", true)
                    put("emi", false)
                    put("paylater", false)
                })
                put("theme", JSONObject().apply {
                    put("color", "#2196F3")
                    put("backdrop_color", "#ffffff")
                })
            }
            checkout.open(this, options)
        } catch (e: Exception) {
            Log.e("PaymentActivity", "Error starting payment", e)
            showError("Unable to start payment. Please try again.")
            progressBar.visibility = View.GONE
        }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        progressBar.visibility = View.GONE
        Log.d("PaymentActivity", "Payment successful: $paymentId")

        if (paymentId.isNullOrEmpty()) {
            showError("Invalid payment details.")
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            showError("User authentication error. Please login again.")
            return
        }

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (!validateAppointment()) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        showError("This time slot is no longer available. Please select another time.")
                    }
                    return@launch
                }

                val saved = saveAppointmentWithTransaction(currentUser, doctorId, selectedTimeSlot, amount, paymentId)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (saved) {
                        showSuccessScreen(paymentId)
                    } else {
                        showError("Failed to save appointment. Please contact support.")
                    }
                }
            } catch (e: Exception) {
                Log.e("PaymentActivity", "Error in appointment flow", e)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showError("An error occurred: ${e.message}")
                }
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        progressBar.visibility = View.GONE
        Log.e("PaymentActivity", "Payment failed: $response")
        val errorMessage = when (code) {
            Checkout.PAYMENT_CANCELED -> "Payment was canceled."
            Checkout.NETWORK_ERROR -> "Network error occurred."
            else -> "Payment failed: $response"
        }
        showError(errorMessage)
    }

    private suspend fun validateAppointment(): Boolean {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return false
        val database = FirebaseDatabase.getInstance().reference
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val appointmentDate = sdf.format(sdf.parse(appointmentDateTime!!))

        val snapshot = database.child("doctors").child(doctorId).child("selectedTimeSlots").get().await()
        val timeSlots = snapshot.children.associate { snap ->
            val slot = snap.getValue(TimeSlot::class.java)
            snap.key!! to slot
        }.filterValues { it != null }.mapValues { it.value!! }

        val matchingSlot = timeSlots.entries.find {
            it.value.id == selectedTimeSlot.id &&
                    it.value.day == selectedTimeSlot.day &&
                    it.value.startTime == selectedTimeSlot.startTime &&
                    it.value.isAvailable == true
        }

        if (matchingSlot == null) {
            Log.w("PaymentActivity", "Selected time slot ${selectedTimeSlot.id} not found or unavailable")
            return false
        }

        val appointmentsSnapshot = database.child("doctors")
            .child(doctorId)
            .child("appointments")
            .orderByChild("date")
            .equalTo(appointmentDate)
            .get().await()

        val conflictingAppointment = appointmentsSnapshot.children.any { snap ->
            val appointment = snap.getValue(Appointment::class.java)
            appointment?.startTime == selectedTimeSlot.startTime
        }

        if (conflictingAppointment) {
            Log.w("PaymentActivity", "Time slot already booked for $appointmentDate")
            return false
        }

        return true
    }

    private suspend fun saveAppointmentWithTransaction(
        currentUser: FirebaseUser,
        doctorId: String,
        selectedTimeSlot: TimeSlot,
        amount: Int,
        paymentId: String
    ): Boolean = withContext(Dispatchers.IO) {
        val database = FirebaseDatabase.getInstance().reference
        val appointmentId = database.push().key ?: return@withContext false

        val appointment = Appointment(
            id = appointmentId,
            doctorId = doctorId,
            patientId = currentUser.uid,
            appointmentDateTime = appointmentDateTime!!,
            timeSlot = selectedTimeSlot,
            amount = amount,
            paymentId = paymentId
        )

        val appointmentMap = mapOf(
            "id" to appointment.id,
            "doctorId" to appointment.doctorId,
            "patientId" to appointment.patientId,
            "date" to appointment.date,
            "day" to appointment.day,
            "timeSlot" to appointment.startTime, // Match rules
            "status" to appointment.status,
            "amount" to appointment.amount,
            "paymentId" to appointment.paymentId,
            "createdAt" to appointment.createdAt,
            "startTime" to appointment.startTime,
            "endTime" to appointment.endTime
        )

        try {
            database.child("patients").child(currentUser.uid)
                .child("appointments").child(appointmentId).setValue(appointmentMap).await()
            database.child("doctors").child(doctorId)
                .child("appointments").child(appointmentId).setValue(appointmentMap).await()
            true
        } catch (e: Exception) {
            Log.e("PaymentActivity", "Failed to save appointment", e)
            false
        }
    }

    private fun showSuccessScreen(paymentId: String?) {
        val intent = Intent(this, AppointmentConfirmedActivity::class.java).apply {
            putExtra("paymentId", paymentId)
            putExtra("doctorName", doctorName)
            putExtra("appointmentTime", appointmentDateTime)
            putExtra("amount", amount)
            putExtra("doctorId", doctorId)
            putExtra("doctorImageUrl", intent.getStringExtra("doctorImageUrl"))
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        super.onBackPressedDispatcher.onBackPressed()
        return true
    }
}

