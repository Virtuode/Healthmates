package com.corps.healthmate.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Rect
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.corps.healthmate.R
import com.corps.healthmate.adapters.TimeSlotAdapter
import com.corps.healthmate.models.DoctorSummary
import com.corps.healthmate.models.TimeSlot
import de.hdodenhof.circleimageview.CircleImageView
import timber.log.Timber
import com.corps.healthmate.utils.AppointmentTimeUtil
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.ContextCompat

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_detail)

        tvTimeRemaining = findViewById(R.id.tvTimeRemaining)
        tvAppointmentDate = findViewById(R.id.tvAppointmentDate)

        val doctor = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("doctor", DoctorSummary::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("doctor")
        }
        Log.d("DoctorDetailActivity", "Received doctor: ${doctor?.toString()}")

        doctor?.let {
            Log.d("DoctorDetailActivity", "Setting doctor details: name=${it.name}, spec=${it.specialization}")

            val profileImageView = findViewById<CircleImageView>(R.id.imageViewProfile)

            if (!it.imageUrl.isNullOrEmpty()) {
                try {
                    val safeImageUrl = it.imageUrl!!.replace("http://", "https://")
                    Glide.with(this)
                        .load(safeImageUrl)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .apply(RequestOptions()
                            .centerCrop()
                            .override(Target.SIZE_ORIGINAL)
                            .format(DecodeFormat.PREFER_ARGB_8888)
                        )
                        .placeholder(R.drawable.userpro)
                        .error(R.drawable.userpro)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.e("DoctorDetailActivity", "Failed to load image from $safeImageUrl", e)
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                Log.d("DoctorDetailActivity", "Image loaded successfully from $dataSource")
                                return false
                            }
                        })
                        .into(object : CustomTarget<Drawable>() {
                            override fun onResourceReady(
                                resource: Drawable,
                                transition: Transition<in Drawable>?
                            ) {
                                profileImageView.setImageDrawable(resource)
                                Log.d("DoctorDetailActivity", "Image set to ImageView")
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                profileImageView.setImageDrawable(placeholder)
                            }
                        })
                } catch (e: Exception) {
                    Log.e("DoctorDetailActivity", "Error setting up image load", e)
                    profileImageView.setImageResource(R.drawable.userpro)
                }
            } else {
                profileImageView.setImageResource(R.drawable.userpro)
            }

            findViewById<TextView>(R.id.tvDoctorName).apply {
                text = it.name ?: "N/A"
                Log.d("DoctorDetailActivity", "Set name to: ${text}")
            }
            findViewById<TextView>(R.id.tvSpecialty).text = it.specialization ?: "N/A"
            findViewById<TextView>(R.id.tvExperience).text = "${it.experience ?: "0"} years"
            findViewById<TextView>(R.id.tvEducation).text = "Education: ${it.education ?: "N/A"}"
            findViewById<TextView>(R.id.tvAvailableDays).text = "Available Days: ${it.availableDays?.joinToString() ?: "N/A"}"
            findViewById<TextView>(R.id.tvBiography).text = "Biography: ${it.biography ?: "N/A"}"
            findViewById<TextView>(R.id.tvEmail).text = "Email: ${it.email ?: "N/A"}"
            findViewById<TextView>(R.id.tvLanguages).text = "Languages: ${it.languages?.joinToString() ?: "N/A"}"
            findViewById<TextView>(R.id.tvPhone).text = "Phone: ${it.phone ?: "N/A"}"

            val rvTimeSlots = findViewById<RecyclerView>(R.id.rvTimeSlots)
            rvTimeSlots.layoutManager = GridLayoutManager(this, 3)
            rvTimeSlots.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val spacing = resources.getDimensionPixelSize(R.dimen.time_slot_spacing)
                    outRect.set(spacing, spacing, spacing, spacing)
                }
            })

            val timeSlotAdapter = TimeSlotAdapter(it.selectedTimeSlots ?: emptyList()) { timeSlot ->
                onTimeSlotSelected(timeSlot, it.availableDays ?: emptyList())
            }
            rvTimeSlots.adapter = timeSlotAdapter

            val fee = specialistFees.find { pair -> pair.first.equals(it.specialization, ignoreCase = true) }?.second ?: 100
            findViewById<TextView>(R.id.feesTxtView).text = "₹ $fee"

            findViewById<Button>(R.id.btnBookAppointment).setOnClickListener {
                if (selectedTimeSlot == null || tvAppointmentDate.text == "Appointment: Not selected") {
                    Toast.makeText(this, "Please select a time slot and date", Toast.LENGTH_SHORT).show()
                } else {
                    val intent = Intent(this, PaymentActivity::class.java).apply {
                        putExtra("amount", fee)
                        putExtra("selectedTimeSlot", selectedTimeSlot)
                        putExtra("doctorId", doctor.id)
                        putExtra("doctorName", doctor.name)
                        putExtra("doctorImageUrl", doctor.imageUrl ?: "")
                        putExtra("appointmentDateTime", appointmentDateTime)
                    }
                    startActivity(intent)
                }
            }
        } ?: run {
            Log.e("DoctorDetailActivity", "Doctor object is null")
            findViewById<TextView>(R.id.tvDoctorName).text = "Doctor data not available"
            findViewById<Button>(R.id.btnBookAppointment).isEnabled = false
        }
    }

    private var appointmentDateTime: String? = null

    private fun onTimeSlotSelected(timeSlot: TimeSlot, availableDays: List<String>) {
        selectedTimeSlot = timeSlot
        Timber.tag("DoctorDetailActivity").d("Selected time slot: $timeSlot")

        val calendar = Calendar.getInstance()
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val selectedDayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)

                // Check if the selected day matches the time slot's day and doctor's availability
                if (selectedDayOfWeek.equals(timeSlot.day, ignoreCase = true) && availableDays.contains(selectedDayOfWeek)) {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    appointmentDateTime = "${dateFormat.format(calendar.time)} ${timeSlot.startTime}"
                    updateAppointmentTime(appointmentDateTime!!)
                } else {
                    Toast.makeText(this, "Please select a ${timeSlot.day} when the doctor is available", Toast.LENGTH_SHORT).show()
                    tvAppointmentDate.text = "Appointment: Not selected"
                    tvAppointmentDate.visibility = View.VISIBLE
                    tvTimeRemaining.visibility = View.GONE
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Optional: Restrict past dates
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000 // Disable past dates
        datePicker.show()
    }





    private fun updateAppointmentTime(appointmentDateTime: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val appointmentTime = sdf.parse(appointmentDateTime)

        val displayFormat = SimpleDateFormat("EEEE, MMMM d, yyyy, HH:mm", Locale.getDefault())
        tvAppointmentDate.text = "Appointment: ${displayFormat.format(appointmentTime)}"
        tvAppointmentDate.visibility = View.VISIBLE

        val timeRemaining = AppointmentTimeUtil.getTimeRemaining(appointmentDateTime)
        val formattedTime = AppointmentTimeUtil.formatTimeRemaining(timeRemaining)

        tvTimeRemaining.apply {
            text = "Time Remaining: $formattedTime"
            visibility = View.VISIBLE
            setTextColor(when {
                timeRemaining.days > 1 -> ContextCompat.getColor(context, R.color.primary)
                timeRemaining.hours > 2 -> ContextCompat.getColor(context, R.color.warning)
                else -> ContextCompat.getColor(context, R.color.urgent)
            })
        }
    }
}