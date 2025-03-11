package com.corps.healthmate.receivers

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.corps.healthmate.database.AppDatabase
import com.corps.healthmate.database.AppDatabase.Companion.getDatabase
import com.corps.healthmate.notification.NotificationHelper
import com.corps.healthmate.utils.ReminderManager

import com.google.firebase.auth.FirebaseAuth
import java.util.*

class ReminderActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra("notification_id", 0)

        when (intent.action) {
            "taken" -> {
                // Cancel the notification
                notificationManager.cancel(notificationId)

                val reminderId = intent.getIntExtra("reminder_id", -1)
                if (reminderId != -1) {
                    handleMedicineTaken(context, reminderId)
                }
            }
            "snooze" -> {
                // Cancel current notification
                notificationManager.cancel(notificationId)

                val reminderId = intent.getIntExtra("reminder_id", -1)
                if (reminderId != -1) {
                    // Get the reminder from database
                    val db = AppDatabase.getDatabase(context)
                    val reminder = db.reminderDao()?.getReminderById(reminderId)

                    if (reminder != null) {
                        // Schedule new alarm for 10 minutes later
                        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                        val newIntent = Intent(context, AlarmReceiver::class.java).apply {
                            putExtra("reminder_id", reminder.id)
                            putExtra("title", "com.corps.healthmate.models.Medicine Reminder")
                            putStringArrayListExtra("pills", ArrayList(reminder.pillNames))
                            putExtra("dosage", reminder.dosage)
                            putExtra("ringtone", reminder.ringtoneUri)
                            putExtra("isHindi", reminder.isHindi)
                            addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
                        }

                        val pendingIntent = PendingIntent.getBroadcast(
                            context,
                            reminder.id + System.currentTimeMillis().toInt(), // Unique request code
                            newIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )

                        val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTime,
                                pendingIntent
                            )
                        } else {
                            alarmManager.setExact(
                                AlarmManager.RTC_WAKEUP,
                                snoozeTime,
                                pendingIntent
                            )
                        }

                        Toast.makeText(
                            context,
                            if (reminder.isHindi) "10 मिनट के लिए स्नूज़" else "Reminder snoozed for 10 minutes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            "dismiss" -> {
                notificationManager.cancel(notificationId)
            }
        }
    }

    private fun handleMedicineTaken(context: Context, reminderId: Int) {
        try {
            val db = AppDatabase.getDatabase(context)
            AppDatabase.databaseWriteExecutor.execute {
                val reminder = db.reminderDao()?.getReminderById(reminderId)
                if (reminder != null) {
                    // Update last taken time
                    reminder.lastTakenTime = System.currentTimeMillis()

                    // Schedule next reminder if it's a repeating reminder
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = reminder.nextTriggerTime
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    reminder.nextTriggerTime = calendar.timeInMillis

                    // Update reminder in database
                    db.reminderDao()?.update(reminder)

                    // Schedule next alarm
                    ReminderManager.getInstance(context).scheduleReminder(reminder)

                    // Show confirmation toast on main thread
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(
                            context,
                            if (reminder.isHindi) "दवा ली गई" else "com.corps.healthmate.models.Medicine taken",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ReminderActionReceiver", "Error handling medicine taken: ${e.message}")
        }
    }
}