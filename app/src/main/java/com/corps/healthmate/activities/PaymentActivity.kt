package com.corps.healthmate.activities

import android.content.Intent
import android.os.Bundle
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import java.io.IOException

class PaymentActivity : AppCompatActivity(), PaymentResultListener {
    private lateinit var progressBar: ProgressBar
    private lateinit var summaryTextView: TextView
    private var amount: Int = 0
    private lateinit var selectedTimeSlot: TimeSlot
    private lateinit var doctorId: String
    private var doctorName: String? = null
    private var appointmentDateTime: String? = null
    private var consultationType: String? = null
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        progressBar = findViewById(R.id.progressBar)
        summaryTextView = findViewById(R.id.tvPaymentSummary)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Payment"

        // Extract intent data
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
        consultationType = intent.getStringExtra("consultationType")

        // Validate inputs
        if (doctorId.isEmpty() || timeSlot == null || appointmentDateTime.isNullOrEmpty() || consultationType.isNullOrEmpty()) {
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
        val appointmentTime = appointmentDateTime?.let { sdf.parse(it) }
        val formattedDateTime = appointmentTime?.let { displayFormat.format(it) } ?: "Invalid Date"

        val summary = """
            Doctor: ${doctorName ?: "N/A"}
            Consultation Type: $consultationType
            Appointment: $formattedDateTime - ${selectedTimeSlot.endTime}
            Amount: ₹$amount
            
            Please complete the payment to confirm your appointment.
        """.trimIndent()
        summaryTextView.text = summary
    }

    private fun startPayment() {
        progressBar.visibility = View.VISIBLE
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_CiRpD8Y4MWq0by") // Replace with your Razorpay key
        checkout.setImage(R.drawable.applogo)

        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                showError("You must be logged in to make a payment.")
                progressBar.visibility = View.GONE
                finish()
                return
            }

            val options = JSONObject().apply {
                put("name", "HealthMate")
                put("description", "Doctor Appointment Payment")
                put("image", "https://your-app-logo-url.png") // Replace with your logo URL
                put("currency", "INR")
                put("amount", amount * 100) // Convert to paise
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
            Timber.e(e, "Error starting payment")
            showError("Unable to start payment. Please try again.")
            progressBar.visibility = View.GONE
        }
    }

    override fun onPaymentSuccess(paymentId: String?) {
        progressBar.visibility = View.GONE

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

                val saved = saveAppointment(currentUser, doctorId, selectedTimeSlot, amount, paymentId, consultationType)
                if (saved) {
                    sendNotificationToDoctor(doctorId, currentUser.displayName ?: "Patient", appointmentDateTime!!, consultationType!!)
                    withContext(Dispatchers.Main) {
                        showSuccessScreen(paymentId)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        showError("Failed to save appointment. Please contact support.")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing payment success")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showError("An error occurred: ${e.message}")
                }
            }
        }
    }

    override fun onPaymentError(code: Int, response: String?) {
        progressBar.visibility = View.GONE
        val errorMessage = when (code) {
            Checkout.PAYMENT_CANCELED -> "Payment was canceled."
            Checkout.NETWORK_ERROR -> "Network error occurred."
            else -> "Payment failed: $response"
        }
        showError(errorMessage)
    }

    private suspend fun validateAppointment(): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val appointmentDate = appointmentDateTime?.let {
            sdf.parse(it)?.let { date -> sdf.format(date) }
        } ?: return false

        // Check existing confirmed appointments for the doctor on this date and time
        val appointmentsSnapshot = database.child("doctors")
            .child(doctorId)
            .child("appointments")
            .orderByChild("date")
            .equalTo(appointmentDate)
            .get().await()

        val hasConflict = appointmentsSnapshot.children.any { snap ->
            val appointment = snap.getValue(Appointment::class.java)
            appointment?.startTime == selectedTimeSlot.startTime &&
                    (appointment.status == "confirmed" || appointment.status == "pending")
        }

        return !hasConflict
    }

    private suspend fun saveAppointment(
        currentUser: FirebaseUser,
        doctorId: String,
        selectedTimeSlot: TimeSlot,
        amount: Int,
        paymentId: String,
        consultationType: String?
    ): Boolean = withContext(Dispatchers.IO) {
        val appointmentId = database.push().key ?: return@withContext false

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val appointmentDate = appointmentDateTime?.let {
            sdf.parse(it)?.let { date -> sdf.format(date) }
        } ?: return@withContext false

        val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val appointmentDay = dayFormat.format(sdf.parse(appointmentDate)!!)

        val appointment = Appointment(
            id = appointmentId,
            doctorId = doctorId,
            patientId = currentUser.uid,
            date = appointmentDate,
            day = appointmentDay,
            startTime = selectedTimeSlot.startTime,
            endTime = selectedTimeSlot.endTime,
            status = "pending", // Doctor must approve
            amount = amount,
            paymentId = paymentId,
            createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            consultationType = consultationType
        )

        val appointmentMap = appointment.toMap()

        try {
            // Multi-path update for atomicity
            val updates = mapOf(
                "patients/${currentUser.uid}/appointments/$appointmentId" to appointmentMap,
                "doctors/$doctorId/appointments/$appointmentId" to appointmentMap,
                "pendingAppointments/$appointmentId" to appointmentMap
            )
            database.updateChildren(updates).await()
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save appointment")
            return@withContext false
        }
    }

    private fun sendNotificationToDoctor(doctorId: String, patientName: String, appointmentTime: String, consultationType: String) {
        val tokenRef = database.child("doctorTokens").child(doctorId)

        tokenRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val token = snapshot.value as? String
                if (token == null) {
                    Timber.w("No FCM token found for doctor $doctorId")
                    return
                }

                val notification = JSONObject().apply {
                    put("to", token)
                    put("notification", JSONObject().apply {
                        put("title", "New Appointment")
                        put("body", "New appointment with $patientName for $consultationType on $appointmentTime")
                        put("click_action", "OPEN_DASHBOARD")
                    })
                    put("data", JSONObject().apply {
                        put("doctorId", doctorId)
                        put("appointmentTime", appointmentTime)
                    })
                }

                val client = OkHttpClient()
                val requestBody = notification.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://fcm.googleapis.com/fcm/send")
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "key=YOUR_FCM_SERVER_KEY") // Replace with your FCM server key
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Timber.e(e, "Failed to send FCM notification")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!it.isSuccessful) {
                                Timber.e("FCM response failed: ${it.body?.string()}")
                            }
                        }
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Failed to fetch doctor token: ${error.message}")
            }
        })
    }

    private fun showSuccessScreen(paymentId: String?) {
        val intent = Intent(this, AppointmentConfirmedActivity::class.java).apply {
            putExtra("paymentId", paymentId)
            putExtra("doctorName", doctorName)
            putExtra("appointmentTime", appointmentDateTime)
            putExtra("amount", amount)
            putExtra("doctorId", doctorId)
            putExtra("doctorImageUrl", intent.getStringExtra("doctorImageUrl"))
            putExtra("consultationType", consultationType)
        }
        startActivity(intent)
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}