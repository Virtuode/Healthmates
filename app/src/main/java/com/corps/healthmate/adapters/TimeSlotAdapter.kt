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
import com.corps.healthmate.models.TimeSlot

class TimeSlotAdapter(
    private val timeSlots: List<TimeSlot>,
    private val onTimeSelected: (TimeSlot) -> Unit
) : RecyclerView.Adapter<TimeSlotAdapter.ViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val timeSlot = timeSlots[position]

        holder.dayText.text = timeSlot.day ?: "N/A"
        holder.timeText.text = "${timeSlot.startTime ?: "N/A"} - ${timeSlot.endTime ?: "N/A"}"
        holder.availabilityText.text = if (timeSlot.isAvailable == true) "Available" else "Booked"

        holder.cardView.setCardBackgroundColor(
            when {
                position == selectedPosition -> ContextCompat.getColor(holder.itemView.context, R.color.colorPrimary)
                timeSlot.isAvailable == false -> ContextCompat.getColor(holder.itemView.context, R.color.grey_200)
                else -> ContextCompat.getColor(holder.itemView.context, R.color.white)
            }
        )

        val textColor = if (position == selectedPosition) {
            ContextCompat.getColor(holder.itemView.context, R.color.white)
        } else {
            ContextCompat.getColor(holder.itemView.context, R.color.black)
        }
        holder.dayText.setTextColor(textColor)
        holder.timeText.setTextColor(textColor)
        holder.availabilityText.setTextColor(textColor)

        holder.itemView.setOnClickListener {
            if (timeSlot.isAvailable == true) {
                val previousPosition = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
                onTimeSelected(timeSlot)
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "This slot is unavailable",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = timeSlots.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.timeSlotCard)
        val dayText: TextView = itemView.findViewById(R.id.dayText)
        val timeText: TextView = itemView.findViewById(R.id.timeText)
        val availabilityText: TextView = itemView.findViewById(R.id.availabilityText)
    }
}