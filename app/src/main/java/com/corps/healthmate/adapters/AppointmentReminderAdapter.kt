package com.corps.healthmate.adapters



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R

import com.corps.healthmate.models.AppointmentReminder
import java.text.SimpleDateFormat
import java.util.*

class AppointmentReminderAdapter : RecyclerView.Adapter<AppointmentReminderAdapter.ViewHolder>() {
    private var appointments = listOf<AppointmentReminder>()

    fun updateAppointments(newAppointments: List<AppointmentReminder>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    override fun getItemCount() = appointments.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.appointment_time)
        private val dateText: TextView = itemView.findViewById(R.id.appointment_date)
        private val doctorName: TextView = itemView.findViewById(R.id.doctor_name)
        private val appointmentType: TextView = itemView.findViewById(R.id.appointment_type)
        private val statusIndicator: ImageView = itemView.findViewById(R.id.status_indicator)

        private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        private val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        fun bind(appointment: AppointmentReminder) {
            val date = Date(appointment.timestamp)
            timeText.text = timeFormat.format(date)
            dateText.text = dateFormat.format(date)
            doctorName.text = appointment.doctorName
            appointmentType.text = appointment.type

            // Update status indicator based on appointment status
            statusIndicator.setImageResource(
                if (appointment.isConfirmed) com.stripe.android.paymentsheet.R.drawable.stripe_ic_paymentsheet_googlepay_primary_button_checkmark
                else R.drawable.baseline_pending_24
            )
        }
    }
}
