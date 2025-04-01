package com.corps.healthmate.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri
import com.corps.healthmate.database.AppDatabase
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.receivers.AlarmReceiver
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

class ReminderManager private constructor(context: Context) {

    private val contextRef = WeakReference(context.applicationContext)
    private val alarmManager: AlarmManager? = contextRef.get()?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val database = contextRef.get()?.let { AppDatabase.getDatabase(it) }

    companion object {
        @Volatile
        private var instance: ReminderManager? = null

        fun getInstance(context: Context): ReminderManager {
            return instance ?: synchronized(this) {
                instance ?: ReminderManager(context).also { instance = it }
            }
        }
    }

    fun scheduleReminder(reminder: Reminder) {
        val safeContext = contextRef.get() ?: return
        val alarmManager = this.alarmManager ?: return

        try {
            cancelReminder(reminder)

            val nextTriggerTime = TimeUtils.getNextTriggerTime(reminder.time)
            reminder.nextTriggerTime = nextTriggerTime

            val intent = Intent(safeContext, AlarmReceiver::class.java).apply {
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
                showExactAlarmPermissionDialog(safeContext)
                return
            }

            val flags =
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(nextTriggerTime, null),
                PendingIntent.getBroadcast(safeContext, reminder.id, intent, flags)
            )

            reminder.isActive = true
            updateReminderInDatabase(reminder)

            Timber.d("Scheduled reminder ${reminder.id} for ${Date(nextTriggerTime)}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to schedule reminder")
        }
    }

    fun cancelReminder(reminder: Reminder) {
        val safeContext = contextRef.get() ?: return
        val alarmManager = this.alarmManager ?: return

        try {
            val intent = Intent(safeContext, AlarmReceiver::class.java)

            val flags =
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE

            val pendingIntent = PendingIntent.getBroadcast(safeContext, reminder.id, intent, flags)

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
            database?.reminderDao()?.update(reminder) ?: Timber.e("Database is null")
        }
    }

    private fun showExactAlarmPermissionDialog(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // API 31+
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
            context.startActivity(intent)
        } else {
            Timber.w("Exact alarm permission request is only needed for Android 12+ (API 31+)")
        }
    }

}
