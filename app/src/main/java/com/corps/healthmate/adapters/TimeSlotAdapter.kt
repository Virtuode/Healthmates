package com.corps.healthmate.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.models.Appointment
import com.corps.healthmate.models.TimeSlot
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import timber.log.Timber

class TimeSlotAdapter(
    private var timeSlots: List<TimeSlot>,
    private var userAppointments: List<Appointment> = emptyList(),
    private val doctorId: String?,
    private val currentUserId: String,
    private var selectedDate: String?,
    private val onTimeSelected: (TimeSlot) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeSlot = timeSlots[position]

        if (timeSlot.day.isNullOrEmpty() || timeSlot.startTime.isNullOrEmpty() || timeSlot.endTime.isNullOrEmpty()) {
            Timber.e("Invalid time slot at position $position: $timeSlot")
            holder.dayText.text = "Invalid"
            holder.timeText.text = "N/A"
            holder.availabilityText.text = "Unavailable"
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.grey_200))
            holder.itemView.isEnabled = false
            return
        }

        holder.dayText.text = timeSlot.day
        holder.timeText.text = "${timeSlot.startTime} - ${timeSlot.endTime}"

        // Check if the slot is booked by the current user
        val isBookedByCurrentUser = userAppointments.any { appointment ->
            appointment.doctorId == doctorId &&
                    appointment.patientId == currentUserId &&
                    appointment.date == selectedDate &&
                    appointment.startTime == timeSlot.startTime &&
                    appointment.endTime == timeSlot.endTime &&
                    appointment.status == "confirmed"
        }

        if (isBookedByCurrentUser) {
            updateSlotUI(holder, timeSlot, "Booked by You", R.color.red_500, R.color.grey_200, false)
            return
        }

        // If no date is selected, mark the slot as unavailable
        if (selectedDate == null || doctorId == null) {
            timeSlot.isAvailable = false
            updateSlotUI(
                holder,
                timeSlot,
                "Unavailable",
                R.color.grey_501,
                R.color.grey_200,
                false
            )
            return
        }

        // Check if the slot is booked by others (server-side)
        FirebaseDatabase.getInstance().getReference("doctors/$doctorId/appointments")
            .orderByChild("date").equalTo(selectedDate)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val isBookedByOthers = snapshot.children.any { dataSnapshot ->
                        val appointment = dataSnapshot.getValue(Appointment::class.java)
                        appointment != null &&
                                appointment.startTime == timeSlot.startTime &&
                                appointment.endTime == timeSlot.endTime &&
                                appointment.patientId != currentUserId &&
                                (appointment.status == "confirmed" || appointment.status == "pending")
                    }

                    if (isBookedByOthers) {
                        updateSlotUI(holder, timeSlot, "Booked", R.color.grey_501, R.color.grey_200, false)
                        return
                    }

                    // Check pending appointments
                    FirebaseDatabase.getInstance().getReference("pendingAppointments")
                        .orderByChild("doctorId").equalTo(doctorId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(pendingSnapshot: DataSnapshot) {
                                val isPendingBooked = pendingSnapshot.children.any { dataSnapshot ->
                                    val appointment = dataSnapshot.getValue(Appointment::class.java)
                                    appointment != null &&
                                            appointment.date == selectedDate &&
                                            appointment.startTime == timeSlot.startTime &&
                                            appointment.endTime == timeSlot.endTime &&
                                            appointment.status == "pending"
                                }

                                timeSlot.isAvailable = !isBookedByCurrentUser && !isBookedByOthers && !isPendingBooked

                                if (isPendingBooked) {
                                    updateSlotUI(holder, timeSlot, "Booked", R.color.grey_501, R.color.grey_200, false)
                                } else {
                                    updateSlotUI(
                                        holder,
                                        timeSlot,
                                        "Available",
                                        R.color.green_201,
                                        if (position == selectedPosition) R.color.colorPrimary else R.color.white,
                                        true
                                    )
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Timber.e("Failed to check pending appointments: ${error.message}")
                                updateSlotUI(holder, timeSlot, "Booked", R.color.grey_501, R.color.grey_200, false)
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    Timber.e("Failed to check doctor appointments: ${error.message}")
                    updateSlotUI(holder, timeSlot, "Booked", R.color.grey_501, R.color.grey_200, false)
                }
            })
    }

    private fun updateSlotUI(
        holder: ViewHolder,
        timeSlot: TimeSlot,
        availabilityText: String,
        availabilityColor: Int,
        backgroundColor: Int,
        isEnabled: Boolean
    ) {
        holder.availabilityText.text = availabilityText
        holder.availabilityText.setTextColor(ContextCompat.getColor(holder.itemView.context, availabilityColor))
        holder.cardView.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, backgroundColor))

        val textColor = if (holder.adapterPosition == selectedPosition && timeSlot.isAvailable) {
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.black)
        }
        holder.dayText.setTextColor(textColor)
        holder.timeText.setTextColor(textColor)

        holder.itemView.isEnabled = isEnabled
        holder.itemView.isClickable = isEnabled
        holder.itemView.setOnClickListener {
            if (!isEnabled) {
                Toast.makeText(
                    holder.itemView.context,
                    if (availabilityText == "Booked by You") "This slot is already booked by you"
                    else "This slot is booked by another patient",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onTimeSelected(timeSlot)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_time_slot, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = timeSlots.size

    fun updateSelectedDate(newSelectedDate: String?) {
        selectedDate = newSelectedDate
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.timeSlotCard)
        val dayText: TextView = itemView.findViewById(R.id.dayText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
        val availabilityText: TextView = itemView.findViewById(R.id.availabilityText)
    }
}