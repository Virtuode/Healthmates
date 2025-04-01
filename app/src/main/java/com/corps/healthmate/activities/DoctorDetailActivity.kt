package com.corps.healthmate.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.AnimationDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.corps.healthmate.R
import com.corps.healthmate.adapters.TimeSlotAdapter
import com.corps.healthmate.models.Appointment
import com.corps.healthmate.models.DoctorSummary
import com.corps.healthmate.models.PendingAppointment
import com.corps.healthmate.models.TimeSlot
import com.corps.healthmate.repository.AppointmentRepository
import com.corps.healthmate.utils.AppointmentTimeUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class DoctorDetailActivity : AppCompatActivity() {

    private val specialistFees = arrayListOf(
        Pair("Allergist/Immunologist", 180),
        Pair("Anesthesiologist", 300),
        Pair("Cardiologist", 220),
        Pair("Dermatologist", 160),
        Pair("Endocrinologist", 200),
        Pair("Gastroenterologist", 230),
        Pair("General Physician", 110),
        Pair("Geriatrician", 190),
        Pair("Hematologist", 240),
        Pair("Infectious Disease Specialist", 250),
        Pair("Nephrologist", 230),
        Pair("Neurologist", 260),
        Pair("Obstetrician/Gynecologist", 200),
        Pair("Oncologist", 280),
        Pair("Ophthalmologist", 190),
        Pair("Orthopedic Surgeon", 300),
        Pair("Otolaryngologist (ENT)", 210),
        Pair("Pediatrician", 190),
        Pair("Physiatrist", 220),
        Pair("Plastic Surgeon", 350),
        Pair("Podiatrist", 170),
        Pair("Psychiatrist", 250),
        Pair("Pulmonologist", 230),
        Pair("Rheumatologist", 210),
        Pair("Urologist", 240),
        Pair("Vascular Surgeon", 320)
    )
    private var selectedTimeSlot: TimeSlot? = null
    private lateinit var tvTimeRemaining: TextView
    private lateinit var tvAppointmentDate: TextView
    private var consultationType: String? = null
    private val repository = AppointmentRepository()
    private val databaseReference = FirebaseDatabase.getInstance().getReference("doctors")
    private val pendingAppointmentsRef = FirebaseDatabase.getInstance().getReference("pendingAppointments")
    private lateinit var progressBar: ProgressBar
    private lateinit var selectedDate: String
    private lateinit var timeSlotAdapter: TimeSlotAdapter
    private lateinit var doctorId: String
    private val timeSlots = mutableListOf<TimeSlot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_detail)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val contentContainer = findViewById<RelativeLayout>(R.id.content_container)
        progressBar = findViewById(R.id.progressBar)
        tvTimeRemaining = findViewById(R.id.tvTimeRemaining)
        tvAppointmentDate = findViewById(R.id.tvAppointmentDate)

        val animationDrawable = contentContainer.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(2000)
        animationDrawable.setExitFadeDuration(2000)
        animationDrawable.start()

        setupSpinner()

        doctorId = intent.getStringExtra("doctorId") ?: ""
        val originalAppointmentId = intent.getStringExtra("originalAppointmentId")

        if (doctorId.isEmpty()) {
            Timber.e("Doctor ID is null or empty")
            Toast.makeText(this, "Invalid doctor ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        fetchDoctorDetails(doctorId, originalAppointmentId)
    }

    private fun setupSpinner() {
        val spinnerConsultationType = findViewById<Spinner>(R.id.spinnerConsultationType)
        val consultationTypes = resources.getStringArray(R.array.consultation_types)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, consultationTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerConsultationType.adapter = adapter

        spinnerConsultationType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                consultationType = if (position == 0) null else consultationTypes[position]
                Timber.d("Selected consultation type: $consultationType")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                consultationType = null
            }
        }
    }

    private fun fetchDoctorDetails(doctorId: String, originalAppointmentId: String?) {
        progressBar.visibility = View.VISIBLE
        databaseReference.child(doctorId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val doctor = parseDoctorSummary(snapshot)
                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    timeSlots.clear()
                    timeSlots.addAll(doctor.selectedTimeSlots)
                    Timber.d("Fetched time slots for doctor $doctorId: $timeSlots")

                    lifecycleScope.launch {
                        try {
                            val userAppointments = repository.getAllAppointments(currentUserId)
                            Timber.d("Fetched user appointments: $userAppointments")
                            val filteredAppointments = userAppointments.filter { it.doctorId == doctorId && it.status == "confirmed" }
                            Timber.d("Filtered user appointments: $filteredAppointments")
                            setupDoctorDetails(doctor, filteredAppointments, currentUserId, originalAppointmentId)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to fetch user appointments")
                            Toast.makeText(this@DoctorDetailActivity, "Failed to load user appointments", Toast.LENGTH_SHORT).show()
                            setupDoctorDetails(doctor, emptyList(), currentUserId, originalAppointmentId)
                        }
                    }
                } else {
                    Timber.e("No data found for doctor ID: $doctorId")
                    Toast.makeText(this@DoctorDetailActivity, "Doctor not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
                progressBar.visibility = View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                Timber.e("Database error: ${error.message}")
                Toast.makeText(this@DoctorDetailActivity, "Failed to load doctor details", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                finish()
            }
        })
    }

    private fun parseDoctorSummary(snapshot: DataSnapshot): DoctorSummary {
        return DoctorSummary(
            id = snapshot.key,
            name = snapshot.child("name").value?.toString(),
            specialization = snapshot.child("specialization").value?.toString(),
            experience = snapshot.child("experience").value?.toString() ?: "0",
            imageUrl = snapshot.child("profilePicture").value?.toString(),
            education = snapshot.child("education").value?.toString(),
            biography = snapshot.child("biography").value?.toString() ?: "",
            availableDays = snapshot.child("availableDays").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList(),
            languages = snapshot.child("languages").getValue(object : GenericTypeIndicator<List<String>>() {}) ?: emptyList(),
            selectedTimeSlots = snapshot.child("selectedTimeSlots").children.mapNotNull { slot ->
                TimeSlot(
                    day = slot.child("day").value?.toString() ?: "",
                    startTime = slot.child("startTime").value?.toString() ?: "",
                    endTime = slot.child("endTime").value?.toString() ?: "",
                    id = slot.child("id").value?.toString() ?: ""
                )
            },
            isVerified = snapshot.child("isVerified").getValue(Boolean::class.java) ?: false,
            createdAt = snapshot.child("createdAt").value?.toString() ?: "",
            documentUrl = snapshot.child("documentUrl").value?.toString() ?: "",
            email = snapshot.child("email").value?.toString() ?: "",
            gender = snapshot.child("gender").value?.toString() ?: "",
            licenseNumber = snapshot.child("licenseNumber").value?.toString() ?: "",
            phone = snapshot.child("phone").value?.toString() ?: ""
        )
    }

    private var appointmentDateTime: String? = null

    private fun setupDoctorDetails(
        doctor: DoctorSummary,
        userAppointments: List<Appointment>,
        currentUserId: String,
        originalAppointmentId: String?
    ) {
        val profileImageView = findViewById<CircleImageView>(R.id.imageViewProfile)
        Glide.with(this)
            .load(doctor.imageUrl?.replace("http://", "https://"))
            .apply(RequestOptions().centerCrop().format(DecodeFormat.PREFER_ARGB_8888))
            .placeholder(R.drawable.userpro)
            .error(R.drawable.userpro)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(profileImageView)

        findViewById<TextView>(R.id.tvDoctorName).text = if (doctor.name?.startsWith("Dr. ") == true) doctor.name else "Dr. ${doctor.name ?: "N/A"}"
        findViewById<TextView>(R.id.tvSpecialty).text = doctor.specialization ?: "N/A"
        findViewById<TextView>(R.id.tvExperience).text = getString(R.string.experience_years, doctor.experience ?: "0")
        findViewById<TextView>(R.id.tvEducation).text = getString(R.string.education, doctor.education ?: "N/A")
        findViewById<TextView>(R.id.tvAvailableDays).text = getString(R.string.available_days, doctor.availableDays.joinToString())
        findViewById<TextView>(R.id.tvBiography).text = getString(R.string.biography, doctor.biography ?: "N/A")
        findViewById<TextView>(R.id.tvLanguages).text = getString(R.string.languages, doctor.languages?.joinToString() ?: "N/A")

        val rvTimeSlots = findViewById<RecyclerView>(R.id.rvTimeSlots)
        rvTimeSlots.layoutManager = GridLayoutManager(this, 3)
        rvTimeSlots.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val spacing = resources.getDimensionPixelSize(R.dimen.time_slot_spacing)
                outRect.set(spacing, spacing, spacing, spacing)
            }
        })

        timeSlotAdapter = TimeSlotAdapter(
            timeSlots = timeSlots,
            userAppointments = userAppointments,
            doctorId = doctor.id!!,
            currentUserId = currentUserId,
            selectedDate = "",
            onTimeSelected = { timeSlot -> onTimeSlotSelected(timeSlot, doctor.availableDays, userAppointments) }
        )
        rvTimeSlots.adapter = timeSlotAdapter

        val fee = specialistFees.find { it.first.equals(doctor.specialization, ignoreCase = true) }?.second ?: 100
        findViewById<Button>(R.id.btnBookAppointment).setOnClickListener {
            if (selectedTimeSlot == null || !this::selectedDate.isInitialized || tvAppointmentDate.text == "Appointment: Not selected") {
                Toast.makeText(this, "Please select a time slot and date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (consultationType == null) {
                Toast.makeText(this, "Please select a consultation type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isDuplicateAppointment(userAppointments, doctorId!!, "$selectedDate ${selectedTimeSlot!!.startTime}")) {
                Toast.makeText(this, "You’ve already booked this time slot with this doctor", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            val doctorAppointmentsRef = FirebaseDatabase.getInstance().getReference("doctors/$doctorId/appointments")
            doctorAppointmentsRef.orderByChild("date").equalTo(selectedDate)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isBookedByOthers = snapshot.children.any { dataSnapshot ->
                            val appointment = dataSnapshot.getValue(Appointment::class.java)
                            appointment != null &&
                                    appointment.startTime == selectedTimeSlot!!.startTime &&
                                    appointment.endTime == selectedTimeSlot!!.endTime &&
                                    (appointment.status == "confirmed" || appointment.status == "pending")
                        }

                        if (isBookedByOthers) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@DoctorDetailActivity, "This time slot is already booked by another patient", Toast.LENGTH_SHORT).show()
                            return
                        }

                        val pendingAppointmentsRef = FirebaseDatabase.getInstance().getReference("pendingAppointments")
                        pendingAppointmentsRef.orderByChild("doctorId").equalTo(doctorId)
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(pendingSnapshot: DataSnapshot) {
                                    progressBar.visibility = View.GONE
                                    val isPendingBooked = pendingSnapshot.children.any { dataSnapshot ->
                                        val appointment = dataSnapshot.getValue(PendingAppointment::class.java)
                                        appointment != null &&
                                                appointment.date == selectedDate &&
                                                appointment.startTime == selectedTimeSlot!!.startTime &&
                                                appointment.status == "pending"
                                    }

                                    if (isPendingBooked) {
                                        Toast.makeText(this@DoctorDetailActivity, "This time slot is already booked by another patient", Toast.LENGTH_SHORT).show()
                                        return
                                    }

                                    val intent = Intent(this@DoctorDetailActivity, PaymentActivity::class.java).apply {
                                        putExtra("amount", fee)
                                        putExtra("selectedTimeSlot", selectedTimeSlot)
                                        putExtra("doctorId", doctor.id)
                                        putExtra("doctorName", doctor.name)
                                        putExtra("doctorImageUrl", doctor.imageUrl ?: "")
                                        putExtra("appointmentDateTime", appointmentDateTime)
                                        putExtra("consultationType", consultationType)
                                        putExtra("originalAppointmentId", originalAppointmentId)
                                    }
                                    startActivity(intent)
                                }

                                override fun onCancelled(error: DatabaseError) {
                                    progressBar.visibility = View.GONE
                                    Timber.e("Failed to check pending appointments: ${error.message}")
                                    Toast.makeText(this@DoctorDetailActivity, "Failed to verify slot availability", Toast.LENGTH_SHORT).show()
                                }
                            })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        progressBar.visibility = View.GONE
                        Timber.e("Failed to check doctor appointments: ${error.message}")
                        Toast.makeText(this@DoctorDetailActivity, "Failed to verify slot availability", Toast.LENGTH_SHORT).show()
                    }
                })
        }

        originalAppointmentId?.let { apptId ->
            userAppointments.find { it.id == apptId }?.let { appt ->
                consultationType = appt.consultationType
                findViewById<Spinner>(R.id.spinnerConsultationType).setSelection(
                    resources.getStringArray(R.array.consultation_types).indexOf(consultationType).takeIf { it >= 0 } ?: 0
                )
            }
        }
    }

    private fun onTimeSlotSelected(timeSlot: TimeSlot, availableDays: List<String>, userAppointments: List<Appointment>) {
        selectedTimeSlot = timeSlot

        val hasExistingBooking = userAppointments.any { appt ->
            appt.doctorId == doctorId &&
                    appt.startTime == timeSlot.startTime &&
                    appt.endTime == timeSlot.endTime &&
                    appt.status == "confirmed"
        }
        if (hasExistingBooking) {
            Toast.makeText(this, "You’ve already booked this time slot on another date", Toast.LENGTH_SHORT).show()
            return
        }

        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDayOfWeek = SimpleDateFormat("EEEE", Locale.ENGLISH).format(calendar.time)
                Timber.d("Selected Day: $selectedDayOfWeek, Slot Day: ${timeSlot.day}, Available Days: $availableDays")
                if (selectedDayOfWeek.equals(timeSlot.day, ignoreCase = true) && availableDays.contains(selectedDayOfWeek)) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    selectedDate = dateFormat.format(calendar.time)
                    appointmentDateTime = "$selectedDate ${timeSlot.startTime}"
                    selectedTimeSlot = timeSlot.copy()
                    updateAppointmentTime(appointmentDateTime!!)
                    timeSlotAdapter.updateSelectedDate(selectedDate)
                } else {
                    Toast.makeText(this, "Please select a ${timeSlot.day} when the doctor is available", Toast.LENGTH_SHORT).show()
                    tvAppointmentDate.text = getString(R.string.appointment_not_selected)
                    tvTimeRemaining.visibility = View.GONE
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun isDuplicateAppointment(
        userAppointments: List<Appointment>,
        doctorId: String,
        appointmentDateTime: String?
    ): Boolean {
        if (appointmentDateTime == null) return false
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val selectedTime = sdf.parse(appointmentDateTime)?.time ?: return false

        return userAppointments.any { appt ->
            appt.doctorId == doctorId && appt.status != "rejected" && appt.status != "missed" &&
                    sdf.parse("${appt.date} ${appt.startTime}")?.time?.let { startTime ->
                        val endTime = sdf.parse("${appt.date} ${appt.endTime}")?.time
                        selectedTime in startTime..(endTime ?: startTime)
                    } == true
        }
    }

    private fun updateAppointmentTime(appointmentDateTime: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val appointmentTime = sdf.parse(appointmentDateTime) ?: Date()
        val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy, HH:mm", Locale.getDefault())
        tvAppointmentDate.text = getString(R.string.appointment_selected, displayFormat.format(appointmentTime))
        tvAppointmentDate.visibility = View.VISIBLE

        val timeRemaining = AppointmentTimeUtil.getTimeRemaining(appointmentDateTime)
        val formattedTime = AppointmentTimeUtil.formatTimeRemaining(timeRemaining)
        tvTimeRemaining.text = getString(R.string.time_remaining, formattedTime)
        tvTimeRemaining.visibility = View.VISIBLE
        tvTimeRemaining.setTextColor(
            when {
                timeRemaining.days > 1 -> ContextCompat.getColor(this, R.color.primary)
                timeRemaining.hours > 2 -> ContextCompat.getColor(this, R.color.warning)
                else -> ContextCompat.getColor(this, R.color.urgent)
            }
        )
    }
}