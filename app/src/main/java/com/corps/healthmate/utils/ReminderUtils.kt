package com.corps.healthmate.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.corps.healthmate.database.AppDatabase
import com.corps.healthmate.database.Reminder
import com.corps.healthmate.receivers.AlarmReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ReminderUtils {
    private const val TAG = "ReminderUtils"

    suspend fun deleteReminder(context: Context, reminder: Reminder) {
        try {
            // Cancel the alarm
            cancelAlarm(context, reminder)

            // Cancel any existing notifications
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(reminder.id)
            notificationManager.cancel(reminder.id + 1000) // Warning notification ID

            // Delete from database
            withContext(Dispatchers.IO) {
                AppDatabase.getDatabase(context).reminderDao()?.delete(reminder)
            }

            Log.d(TAG, "Successfully deleted reminder with ID: ${reminder.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reminder", e)
        }
    }

    private fun cancelAlarm(context: Context, reminder: Reminder) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                ?: throw IllegalStateException("Could not get AlarmManager service")
            
            // Create the same intent that was used to set the alarm
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("reminder_id", reminder.id)
                putExtra("title", "com.corps.healthmate.models.Medicine Reminder")
                putStringArrayListExtra("pills", ArrayList(reminder.pillNames))
                putExtra("dosage", reminder.dosage)
                putExtra("ringtone", reminder.ringtoneUri)
                putExtra("isHindi", reminder.isHindi)
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }

            // Create the pending intent with UPDATE flag to match existing one
            val pendingIntent = try {
                PendingIntent.getBroadcast(
                    context,
                    reminder.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create PendingIntent for reminder ${reminder.id}", e)
                return
            }

            // Cancel the alarm
            try {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel alarm for reminder ${reminder.id}", e)
                return
            }
            
            // For extra safety, try to cancel any exact alarms on Android S and above
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                try {
                    if (alarmManager.canScheduleExactAlarms()) {
                        try {
                            // Use reflection to safely call cancelAlarmClock if available
                            val method = alarmManager.javaClass.getMethod("cancelAlarmClock", PendingIntent::class.java)
                            method.invoke(alarmManager, pendingIntent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Method cancelAlarmClock not available", e)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to cancel exact alarm for reminder ${reminder.id}", e)
                }
            }

            Log.d(TAG, "Successfully cancelled all alarms for reminder ID: ${reminder.id}")

        } catch (e: Exception) {
            Log.e(TAG, "Error in cancelAlarm for reminder ${reminder.id}", e)
            throw e // Re-throw to be handled by caller
        }
    }
}
