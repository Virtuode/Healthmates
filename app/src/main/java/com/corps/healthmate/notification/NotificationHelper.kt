package com.corps.healthmate.notification

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.corps.healthmate.R
import com.corps.healthmate.receivers.ReminderActionReceiver
import com.corps.healthmate.utils.VoiceNotificationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class NotificationHelper(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val voiceHelper = VoiceNotificationHelper(context)
    
    companion object {
        const val CHANNEL_ID = "medicine_reminder_channel"
        const val CHANNEL_ID_WARNING = "medicine_warning_channel"
        private const val NOTIFICATION_ID = 1
        private const val WARNING_NOTIFICATION_ID = 2
        private const val WAKELOCK_TIMEOUT = 30 * 60 * 1000L // 30 minutes
        private const val VOICE_REPEAT_INTERVAL = 60 * 1000L // 1 minute
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Main reminder channel with alarm importance
            val mainChannel = NotificationChannel(
                CHANNEL_ID,
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for medicine reminders"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                setShowBadge(true)
                setBypassDnd(true)
                
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                setSound(null, audioAttributes)
            }

            // Warning channel for 5-min alerts
            val warningChannel = NotificationChannel(
                CHANNEL_ID_WARNING,
                "Reminder Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "5-minute warning notifications"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(mainChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    fun showWarningPopup(title: String, pills: List<String>, isHindi: Boolean = false) {
        // Set language for voice notification
        voiceHelper.setLanguage(isHindi)
        
        // Speak warning message
        if (isHindi) {
            voiceHelper.speakText("5 मिनट में दवाई लेने का समय होगा. ${pills.joinToString(", ") { "दवाई $it" }}")
        } else {
            voiceHelper.speakText("Reminder in 5 minutes to take: ${pills.joinToString(", ")}")
        }

        // Show dialog
        val activity = context as? Activity ?: return
        activity.runOnUiThread {
            MaterialAlertDialogBuilder(context, R.style.MaterialAlertDialog_Rounded)
                .setTitle("⏰ ${if (isHindi) "आगामी रिमाइंडर" else "Upcoming Reminder"}")
                .setMessage(
                    if (isHindi) {
                        "5 मिनट में आपको यह दवाई लेनी है:\n${pills.joinToString("\n") { "• $it" }}"
                    } else {
                        "In 5 minutes you need to take:\n${pills.joinToString("\n") { "• $it" }}"
                    }
                )
                .setPositiveButton(if (isHindi) "ठीक है" else "Got it") { dialog, _ -> dialog.dismiss() }
                .setNegativeButton(if (isHindi) "बाद में" else "Snooze") { _, _ ->
                    Toast.makeText(
                        context, 
                        if (isHindi) "5 मिनट के लिए स्नूज़ किया गया" else "Reminder snoozed for 5 minutes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setCancelable(true)
                .show()
        }

        // Show warning notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_WARNING)
            .setContentTitle(if (isHindi) "दवाई रिमाइंडर" else "Medicine Reminder")
            .setContentText(
                if (isHindi) {
                    "5 मिनट में दवाई लेनी है: ${pills.joinToString(", ")}"
                } else {
                    "Medicine due in 5 minutes: ${pills.joinToString(", ")}"
                }
            )
            .setSmallIcon(R.drawable.outline_notifications_24)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 250, 250, 250))
            .build()

        notificationManager.notify(WARNING_NOTIFICATION_ID, notification)
    }

    fun sendReminderNotification(title: String?, pills: List<String>, soundUri: Uri, isHindi: Boolean = false) {
        val channelId = CHANNEL_ID
        val reminderId = System.currentTimeMillis().toInt()

        // Create notification content
        val pillsText = pills.joinToString(", ")
        val contentText = if (isHindi) {
            "दवाई का समय: $pillsText"
        } else {
            "Time to take: $pillsText"
        }

        // Acquire wake lock for longer duration
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "HealthMate:ReminderWakeLock"
        )
        wakeLock.acquire(WAKELOCK_TIMEOUT)

        try {
            // Setup voice reminder only once, not repeating
            if (isHindi) {
                voiceHelper.speakText("दवाई लेने का समय हो गया है. ${pills.joinToString(", ") { "दवाई $it" }}")
            } else {
                voiceHelper.speakText("Time to take your medicine: ${pills.joinToString(", ")}")
            }

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.baseline_medication_liquid_24)
                .setContentTitle(title)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(soundUri)
                .setAutoCancel(false) // Don't auto cancel
                .setOngoing(true) // Make persistent
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(
                    R.drawable.ic_check,
                    if (isHindi) "ले लिया" else "Taken",
                    createActionPendingIntent("taken", reminderId)
                )
                .addAction(
                    R.drawable.ic_snooze_24,
                    if (isHindi) "स्नूज़" else "Snooze",
                    createActionPendingIntent("snooze", reminderId)
                )
                .addAction(
                    R.drawable.ic_cancel_24,
                    if (isHindi) "रद्द करें" else "Dismiss",
                    createActionPendingIntent("dismiss", reminderId)
                )
                .build()

            // Make notification persistent and insistent (will repeat sound)
            notification.flags = notification.flags or 
                Notification.FLAG_INSISTENT or // Makes sound repeat
                Notification.FLAG_NO_CLEAR or // Makes notification persistent
                Notification.FLAG_ONGOING_EVENT // Shows in ongoing section

            notificationManager.notify(reminderId, notification)

        } finally {
            // Wake lock will be released when the notification is dismissed
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun createActionPendingIntent(action: String, reminderId: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra("reminder_id", reminderId)
            putExtra("notification_id", reminderId)
        }
        
        return PendingIntent.getBroadcast(
            context,
            reminderId + action.hashCode(), // Unique request code for each action
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun onDestroy() {
        voiceHelper.shutdown()
    }
}