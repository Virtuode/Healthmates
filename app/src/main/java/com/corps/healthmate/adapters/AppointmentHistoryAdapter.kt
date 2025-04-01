package com.corps.healthmate.adapters


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.models.Appointment
import java.util.Locale

class AppointmentHistoryAdapter(
    private val appointments: List<Appointment>
) : RecyclerView.Adapter<AppointmentHistoryAdapter.HistoryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.bind(appointment)
    }

    override fun getItemCount(): Int = appointments.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDoctorName: TextView = itemView.findViewById(R.id.tv_doctor_name)
        private val tvDateTime: TextView = itemView.findViewById(R.id.tv_date_time)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_status)

        fun bind(appointment: Appointment) {
            tvDoctorName.text = "Doctor: ${appointment.doctorId}" // Ideally fetch doctor name
            tvDateTime.text = "${appointment.date} ${appointment.startTime}"
            tvStatus.text = appointment.status.capitalize(Locale.ROOT)
        }
    }
}