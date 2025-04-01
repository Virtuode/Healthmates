package com.corps.healthmate.utils

import android.content.Context
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.corps.healthmate.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import timber.log.Timber

object TimeUtils {
    private const val URGENT_THRESHOLD_MINUTES = 60

    fun calculateTimeDifference(reminderTime: String, textView: TextView, isTaken: Boolean = false) {
        try {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentCalendar = Calendar.getInstance()
            val reminderCalendar = Calendar.getInstance()
            
            val time = timeFormat.parse(reminderTime)
            if (time != null) {
                val tempCalendar = Calendar.getInstance().apply { 
                    this.time = time 
                }
                reminderCalendar.apply {
                    set(Calendar.HOUR_OF_DAY, tempCalendar.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, tempCalendar.get(Calendar.MINUTE))
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                if (isTaken || reminderCalendar.before(currentCalendar)) {
                    reminderCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                val diffMillis = reminderCalendar.timeInMillis - currentCalendar.timeInMillis
                updateTimeDisplay(textView.context, textView, diffMillis)
                
                Timber.d("Time calculation: Reminder=$reminderTime, Current=${timeFormat.format(currentCalendar.time)}, IsTaken=$isTaken")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating time difference")
        }
    }

    private fun updateTimeDisplay(context: Context, textView: TextView, diffMillis: Long) {
        val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
        val hours = diffMinutes / 60
        val minutes = diffMinutes % 60

        val timeRemaining = when {
            diffMinutes <= 0 -> "Time to take medicine!"
            hours > 0 -> {
                if (minutes > 0) {
                    "${hours}h ${minutes}m remaining"
                } else {
                    "${hours}h remaining"
                }
            }
            else -> "${minutes}m remaining"
        }

        val colorRes = when {
            diffMinutes <= 0 -> R.color.red_500
            diffMinutes <= URGENT_THRESHOLD_MINUTES -> R.color.yellow_700
            else -> R.color.green_201
        }

        textView.text = timeRemaining
        textView.setTextColor(ContextCompat.getColor(context, colorRes))
    }

    fun getNextTriggerTime(reminderTime: String): Long {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentCalendar = Calendar.getInstance()
        val reminderCalendar = Calendar.getInstance()

        val time = timeFormat.parse(reminderTime)
        if (time != null) {
            reminderCalendar.apply {

                @Suppress("DEPRECATION")
                set(Calendar.HOUR_OF_DAY, time.hours)
                @Suppress("DEPRECATION")
                set(Calendar.MINUTE, time.minutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (reminderCalendar.before(currentCalendar)) {
                reminderCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            return reminderCalendar.timeInMillis
        }
        return currentCalendar.timeInMillis
    }

    fun getRelativeTime(timestamp: Long): String {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = currentTime - timestamp

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis)
        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis)
        val days = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        return when {
            seconds < 60 -> "Now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "$days day ago"
        }
    }
}