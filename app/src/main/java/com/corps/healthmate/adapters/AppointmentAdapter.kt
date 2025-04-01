package com.corps.healthmate.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.corps.healthmate.R
import com.corps.healthmate.activities.VideoCallActivity
import com.corps.healthmate.models.Appointment
import com.corps.healthmate.models.DisplayAppointment
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.FirebaseDatabase
import timber.log.Timber
import java.util.Date

class AppointmentAdapter(
    private var appointments: List<DisplayAppointment>,
    private val currentDate: Date,
    private val onAppointmentClick: ((Appointment) -> Unit)? = null
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_appointment_reminder, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        holder.bind(appointments[position], currentDate)
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(newAppointments: List<DisplayAppointment>, newCurrentDate: Date) {
        appointments = newAppointments
        currentDate.time = newCurrentDate.time
        notifyDataSetChanged()
    }

    inner class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage = itemView.findViewById<ImageView>(R.id.profile_image)
        private val doctorName = itemView.findViewById<TextView>(R.id.doctor_name)
        private val appointmentTime = itemView.findViewById<TextView>(R.id.appointment_time)
        private val appointmentType = itemView.findViewById<TextView>(R.id.appointment_type)
        private val appointmentStatus = itemView.findViewById<TextView>(R.id.appointment_status)
        private val joinButton = itemView.findViewById<MaterialButton>(R.id.join_call_button)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) onAppointmentClick?.invoke(appointments[position].appointment)
            }
        }

        fun bind(displayAppointment: DisplayAppointment, currentDate: Date) {
            val appointment = displayAppointment.appointment
            Glide.with(itemView.context).load(displayAppointment.doctorImageUrl ?: R.drawable.user).into(profileImage)
            doctorName.text = itemView.context.getString(R.string.doctor_name_format, displayAppointment.doctorName ?: "Unknown")
            appointmentTime.text = "${appointment.date} ${appointment.startTime}"
            appointmentType.text = appointment.consultationType ?: "Consultation"

            when (appointment.status.lowercase()) {
                Appointment.STATUS_MISSED -> bindMissedStatus()
                Appointment.STATUS_COMPLETED -> bindCompletedStatus()
                Appointment.STATUS_CONFIRMED, Appointment.STATUS_ONGOING -> bindConfirmedOrOngoingStatus(appointment, displayAppointment, currentDate)
                Appointment.STATUS_PENDING -> bindPendingStatus()
                else -> bindDefaultStatus(appointment)
            }
        }

        private fun bindPendingStatus() {
            appointmentStatus.text = "Pending"
            appointmentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.yellow_700))
            joinButton.visibility = View.GONE
        }

        private fun bindMissedStatus() {
            appointmentStatus.text = "Missed"
            appointmentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.red_500))
            joinButton.visibility = View.GONE
        }

        private fun bindCompletedStatus() {
            appointmentStatus.text = "Completed"
            appointmentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.grey_501))
            joinButton.visibility = View.GONE
        }

        private fun bindConfirmedOrOngoingStatus(appointment: Appointment, displayAppointment: DisplayAppointment, currentDate: Date) {
            val isOngoing = appointment.isOngoing(currentDate)
            if (isOngoing) {
                appointmentStatus.text = "Ongoing"
                appointmentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.green_201))
                joinButton.visibility = View.VISIBLE
                joinButton.isEnabled = true
                joinButton.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.colorPrimary)
                joinButton.setOnClickListener {
                    val chatId = displayAppointment.chatId ?: createChat(appointment)
                    startVideoCall(chatId, displayAppointment.doctorName)
                }
            } else {
                val timeRemaining = appointment.getTimeRemaining(currentDate)
                appointmentStatus.text = timeRemaining
                appointmentStatus.setTextColor(ContextCompat.getColor(itemView.context, if (timeRemaining == "Expired") R.color.red_500 else R.color.green_201))
                joinButton.visibility = View.GONE
            }
        }

        private fun bindDefaultStatus(appointment: Appointment) {
            appointmentStatus?.text = appointment.status.capitalize()
            appointmentStatus.setTextColor(ContextCompat.getColor(itemView.context, R.color.secondary_text))
            joinButton.visibility = View.GONE
        }

        private fun createChat(appointment: Appointment): String {
            val db = FirebaseDatabase.getInstance().reference
            val chatRef = db.child("chats").push()
            val chatId = chatRef.key ?: return ""
            chatRef.setValue(mapOf("appointmentId" to appointment.id, "doctorId" to appointment.doctorId, "patientId" to appointment.patientId, "active" to true, "videoCallInitiated" to false))
            return chatId
        }

        private fun startVideoCall(roomId: String, doctorName: String?) {
            val intent = Intent(itemView.context, VideoCallActivity::class.java).apply { putExtra("roomID", roomId) }
            itemView.context.startActivity(intent)
            Toast.makeText(itemView.context, "Joining call with ${doctorName ?: "Unknown"}", Toast.LENGTH_SHORT).show()
        }
    }
}