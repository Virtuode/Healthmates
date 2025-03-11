package com.corps.healthmate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.corps.healthmate.notification.NotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val title = intent.getStringExtra("title") ?: "com.corps.healthmate.models.Medicine Reminder"
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
            
            // Show notification with sound, vibration, and voice
            notificationHelper.sendReminderNotification(title, pillNames, alarmSound, isHindi)
            
            // Log successful alarm trigger
            Log.d("AlarmReceiver", "Alarm triggered for: $pillNames")
            
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Error processing alarm", e)
        }
    }
}
