package com.corps.healthmate.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.corps.healthmate.database.AppDatabase
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.receivers.AlarmReceiver
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class ReminderManager private constructor(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val database = AppDatabase.getDatabase(context)

    companion object {
        @Volatile
        private var instance: ReminderManager? = null
        private const val TAG = "ReminderManager"

        fun getInstance(context: Context): ReminderManager {
            return instance ?: synchronized(this) {
                instance ?: ReminderManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun scheduleReminder(reminder: Reminder) {
        try {
            cancelReminder(reminder)

            val nextTriggerTime = TimeUtils.getNextTriggerTime(reminder.time)
            reminder.nextTriggerTime = nextTriggerTime

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("reminder_id", reminder.id)
                putExtra("title", "Medicine Reminder")
                putStringArrayListExtra("pills", ArrayList(reminder.pillNames))
                putExtra("dosage", reminder.dosage)
                putExtra("ringtone", reminder.ringtoneUri)
                putExtra("isHindi", reminder.isHindi)
                putExtra("scheduled_time", nextTriggerTime)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Timber.w("Cannot schedule exact alarms - missing permission")
                return
            }

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextTriggerTime, null),
                PendingIntent.getBroadcast(
                    context,
                    reminder.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            reminder.isActive = true
            updateReminderInDatabase(reminder)
            
            Timber.d("Scheduled reminder ${reminder.id} for ${Date(nextTriggerTime)}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule reminder")
        }
    }

    fun cancelReminder(reminder: Reminder) {
        try {
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("reminder_id", reminder.id)
                putExtra("title", "com.corps.healthmate.models.Medicine Reminder")
                putStringArrayListExtra("pills", ArrayList(reminder.pillNames))
                putExtra("dosage", reminder.dosage)
                putExtra("ringtone", reminder.ringtoneUri)
                putExtra("isHindi", reminder.isHindi)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_NO_CREATE
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                flags
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }

            reminder.isActive = false
            updateReminderInDatabase(reminder)

            Timber.d("Cancelled reminder ${reminder.id}")
        } catch (e: Exception) {
            Timber.e(e, "Error cancelling reminder ${reminder.id}")
        }
    }

    private fun updateReminderInDatabase(reminder: Reminder) {
        AppDatabase.databaseWriteExecutor.execute {
            try {
                database!!.reminderDao()!!.update(reminder)
            } catch (e: Exception) {
                Timber.e(e, "Error updating reminder in database")
            }
        }
    }
}
