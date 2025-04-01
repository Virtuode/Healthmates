package com.corps.healthmate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import com.corps.healthmate.notification.NotificationHelper
import timber.log.Timber

class AlarmReceiver : BroadcastReceiver() {

    companion object {

        const val ACTION_MEDICATION_ALARM = "ACTION_MEDICATION_ALARM"

    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == null || !isExpectedAction(action)) {
            Timber.tag("AlarmReceiver").w("Received intent with unexpected action: $action")
            return
        }

        try {
            val title = intent.getStringExtra("title") ?: "Medicine Reminder"
            val pillNames = intent.getStringArrayListExtra("pills") ?: arrayListOf()
            val isHindi = intent.getBooleanExtra("isHindi", false)

            // Create notification helper
            val notificationHelper = NotificationHelper(context)

            // Get default alarm sound
            val uriString = intent.getStringExtra("ringtone")
            val alarmSound = if (!uriString.isNullOrEmpty()) {
                Uri.parse(uriString)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }
            notificationHelper.sendReminderNotification(title, pillNames, alarmSound, isHindi)

        } catch (e: Exception) {
            Timber.tag("AlarmReceiver").e(e, "Error processing alarm")
        }
    }

    private fun isExpectedAction(action: String): Boolean {
        // This function validates if the received action is one we expect
        return when (action) {
            ACTION_MEDICATION_ALARM -> true
            Intent.ACTION_BOOT_COMPLETED -> true
            // Add any other system broadcasts your receiver handles
            // Intent.ACTION_TIMEZONE_CHANGED -> true
            // Intent.ACTION_TIME_CHANGED -> true
            else -> false
        }
    }
}