package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.models.Appointment
import com.corps.healthmate.utils.AppointmentTimeUtil

class AppointmentAdapter : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {
    private var appointments = listOf<Appointment>()

    fun updateAppointments(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position])
    }

    override fun getItemCount() = appointments.size

    class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvTimeRemaining: TextView = view.findViewById(R.id.tvTimeRemaining)
        private val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        private val tvDoctorName: TextView = view.findViewById(R.id.tvDoctorName)
        private val tvAppointmentTime: TextView = view.findViewById(R.id.tvAppointmentTime)

        fun bind(appointment: Appointment) {
            // Set basic appointment details
            tvDoctorName.text = appointment.doctorName
            tvAppointmentTime.text = "${appointment.date} ${appointment.startTime}"
            
            // Calculate and display time remaining
            val appointmentDateTime = "${appointment.date} ${appointment.startTime}"
            val timeRemaining = AppointmentTimeUtil.getTimeRemaining(appointmentDateTime)
            val formattedTime = AppointmentTimeUtil.formatTimeRemaining(timeRemaining)
            tvTimeRemaining.text = formattedTime
            
            // Update status and its color based on appointment proximity
            if (AppointmentTimeUtil.isAppointmentNear(appointmentDateTime)) {
                tvStatus.apply {
                    text = "Starting Soon"
                    setTextColor(ContextCompat.getColor(context, R.color.urgent))
                }
            } else {
                tvStatus.apply {
                    text = appointment.status ?: "Scheduled"
                    setTextColor(ContextCompat.getColor(context, when(appointment.status) {
                        "completed" -> R.color.normal_time_color
                        "cancelled" -> R.color.overdue_color
                        else -> R.color.primary
                    }))
                }
            }
        }
    }
} 