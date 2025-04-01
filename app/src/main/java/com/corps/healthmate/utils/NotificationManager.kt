package com.corps.healthmate.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.corps.healthmate.R
import com.corps.healthmate.activities.MainActivity

object NotificationManager {
    private const val CHANNEL_ID = "appointment_channel"
    private const val CHANNEL_NAME = "Appointment Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for upcoming appointments"
    
    fun scheduleAppointmentReminders(context: Context, appointmentTime: String) {
        // Schedule multiple reminders:
        // 1. 24 hours before
        // 2. 1 hour before
        // 3. 15 minutes before
        val timeRemaining = AppointmentTimeUtil.getTimeRemaining(appointmentTime)
        
        if (timeRemaining.isUpcoming) {
            when {
                timeRemaining.days == 1L -> sendNotification(
                    context,
                    "com.corps.healthmate.models.Appointment Tomorrow",
                    "You have an appointment scheduled for tomorrow"
                )
                timeRemaining.hours == 1L -> sendNotification(
                    context,
                    "com.corps.healthmate.models.Appointment Soon",
                    "Your appointment is in 1 hour"
                )
                timeRemaining.minutes == 15L -> sendNotification(
                    context,
                    "com.corps.healthmate.models.Appointment Alert",
                    "Your appointment starts in 15 minutes"
                )
            }
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel(notificationManager)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableLights(true)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }
} 