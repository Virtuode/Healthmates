package com.corps.healthmate.adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.corps.healthmate.R
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.filesImp.ReminderDiffCallback
import com.corps.healthmate.interfaces.TimeDifferenceCallback
import com.corps.healthmate.utils.TimeUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class ReminderAdapter(
    private val listener: OnReminderClickListener,  // Use the interface here
    private val callback: TimeDifferenceCallback  // Callback for calculating time remaining
) : RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder>() {

    private var reminders: List<Reminder> = emptyList()

    fun updateReminders(newReminders: List<Reminder>) {
        val diffCallback = ReminderDiffCallback(reminders, newReminders)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        
        this.reminders = newReminders
        diffResult.dispatchUpdatesTo(this)
    }

    // Interface for handling clicks on delete and deactivate buttons
    interface OnReminderClickListener {
        fun onDeleteClick(reminder: Reminder?)  // Method to handle delete button clicks
        fun onDeactivateClick(reminder: Reminder?)  // Method to handle deactivate button clicks
        fun onMedicineTaken(reminder: Reminder?)  // Add this method
    }

    // Create a new view for each reminder item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ReminderViewHolder(view)
    }

    // Bind data to the view holder for each reminder item
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        try {
            val reminder = reminders[position]
            
            // Convert 24-hour time to 12-hour format
            val timeFormat24 = SimpleDateFormat("HH:mm", Locale.getDefault())
            val timeFormat12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val time24 = timeFormat24.parse(reminder.time)
            val time12 = time24?.let { timeFormat12.format(it) } ?: reminder.time
            
            holder.bind(reminder, time12)
        } catch (e: Exception) {
            Timber.e(e, "Error binding reminder")
        }
    }

    // Return the total number of items in the list
    override fun getItemCount(): Int = reminders.size

    // ViewHolder for each reminder item in the RecyclerView
    inner class ReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val reminderTime: TextView = itemView.findViewById(R.id.reminderTime)
        private val reminderDay: TextView = itemView.findViewById(R.id.reminderDay)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        private val pillNamesChipGroup: ChipGroup = itemView.findViewById(R.id.pillNamesChipGroup)
        private val timeRemainingText: TextView = itemView.findViewById(R.id.timeRemainingText)

        fun bind(reminder: Reminder, formattedTime: String) {
            try {
                reminderTime.text = formattedTime
                reminderDay.text = reminder.day
                
                // Calculate time remaining with isTaken = false for normal display
                TimeUtils.calculateTimeDifference(reminder.time, timeRemainingText, isTaken = false)
                
                setupPillNames(reminder.pillNames)
                setupButtons(reminder)
            } catch (e: Exception) {
                Timber.e(e, "Error binding reminder")
            }
        }

        private fun setupPillNames(pillNames: List<String>) {
            pillNamesChipGroup.removeAllViews()
            
            pillNames.forEach { pillName ->
                addPillChip(pillName, null)
            }
        }

        private fun addPillChip(pillName: String, dosage: String?) {
            val chip = Chip(itemView.context).apply {
                text = buildString {
                    append(pillName)
                    if (!dosage.isNullOrBlank()) {
                        append(" (")
                        append(dosage)
                        append(")")
                    }
                }
                isClickable = false
                isCheckable = false
                setChipBackgroundColorResource(R.color.default_chip_background)
                setTextColor(ContextCompat.getColor(context, R.color.white))
                chipStrokeWidth = 0f
                shapeAppearanceModel = shapeAppearanceModel.toBuilder()
                    .setAllCornerSizes(resources.getDimension(R.dimen.chip_corner_radius))
                    .build()
            }
            pillNamesChipGroup.addView(chip)
        }

        private fun setupButtons(reminder: Reminder) {
            deleteButton.setOnClickListener {
                animateButton(deleteButton) {
                    listener.onDeleteClick(reminder)
                }
            }
        }

        private fun animateButton(button: View, onEnd: () -> Unit) {
            ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.8f, 1f).apply {
                duration = 200
                doOnEnd { onEnd() }
                start()
            }
        }

        fun updateTimeRemaining(reminder: Reminder, isTaken: Boolean) {
            TimeUtils.calculateTimeDifference(reminder.time, timeRemainingText, isTaken)
        }
    }

    companion object {
        private const val TAG = "ReminderAdapter"
    }
}