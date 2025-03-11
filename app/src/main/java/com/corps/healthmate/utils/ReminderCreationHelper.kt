package com.corps.healthmate.utils

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.corps.healthmate.R
import com.corps.healthmate.database.AppDatabase
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.databinding.DialogAddPillBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

object ReminderCreationHelper {
    fun showAddPillDialog(context: Context, userId: String, onReminderCreated: () -> Unit) {
        val binding = DialogAddPillBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
            .setTitle("Add New Reminder")
            .setView(binding.root)
            .create()

        // Set up frequency spinner
        val frequencies = arrayOf("Daily", "Weekly", "Monthly", "Once")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, frequencies)
        binding.frequencySpinner.adapter = adapter

        // Time picker setup
        binding.timePickerButton.setOnClickListener {
            showTimePicker(context) { selectedTime ->
                binding.timePickerButton.text = selectedTime
            }
        }

        binding.saveButton.setOnClickListener {
            val pillName = binding.pillNameEditText.text.toString()
            val dosage = binding.dosageEditText.text.toString()
            val time = binding.timePickerButton.text.toString()
            val frequency = binding.frequencySpinner.selectedItem.toString().lowercase()
            val isHindi = binding.hindiRadio.isChecked

            if (validateInput(context, pillName, dosage, time)) {
                createReminder(context, userId, pillName, dosage, time, frequency, isHindi, onReminderCreated)
                dialog.dismiss()
            }
        }

        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showTimePicker(context: Context, onTimeSelected: (String) -> Unit) {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(12)
            .setMinute(0)
            .setTitleText("Select Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val hour = picker.hour
            val minute = picker.minute
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val formattedTime = timeFormat.format(calendar.time)
            onTimeSelected(formattedTime)
        }

        picker.show((context as androidx.fragment.app.FragmentActivity).supportFragmentManager, "time_picker")
    }

    private fun validateInput(context: Context, pillName: String, dosage: String, time: String): Boolean {
        when {
            pillName.isBlank() -> {
                Toast.makeText(context, "Please enter pill name", Toast.LENGTH_SHORT).show()
                return false
            }
            dosage.isBlank() -> {
                Toast.makeText(context, "Please enter dosage", Toast.LENGTH_SHORT).show()
                return false
            }
            time == "Select Time" -> {
                Toast.makeText(context, "Please select time", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun createReminder(
        context: Context,
        userId: String,
        pillName: String,
        dosage: String,
        time: String,
        frequency: String,
        isHindi: Boolean,
        onReminderCreated: () -> Unit
    ) {
        // Calculate next trigger time
        val calendar = Calendar.getInstance()
        val timeParts = time.split(":")
        calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
        calendar.set(Calendar.MINUTE, timeParts[1].toInt())
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // If time is in past, set it for next occurrence
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            when (frequency) {
                "daily" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
                "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "monthly" -> calendar.add(Calendar.MONTH, 1)
                else -> {} // For one-time reminders, keep as is
            }
        }

        val reminder = Reminder(
            userId = userId,
            pillNames = listOf(pillName),
            time = time,
            day = frequency,
            dosage = if (dosage.isBlank()) null else dosage,
            isActive = true,
            isHindi = isHindi,
            frequency = frequency,
            nextTriggerTime = calendar.timeInMillis
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                AppDatabase.getDatabase(context).reminderDao()?.insert(reminder)
                ReminderManager.getInstance(context).scheduleReminder(reminder)
                withContext(Dispatchers.Main) {
                    val message = if (isHindi) {
                        "दवाई का रिमाइंडर सेट हो गया है"
                    } else {
                        "Reminder created successfully"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    onReminderCreated()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error creating reminder: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
