package com.corps.healthmate.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import com.corps.healthmate.notification.NotificationHelper

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title")
        val pills = intent.getStringArrayListExtra("pills") ?: arrayListOf()
        val uriString = intent.getStringExtra("ringtone")
        val soundUri = if (!uriString.isNullOrEmpty()) {
            Uri.parse(uriString)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }

        val notificationHelper = NotificationHelper(context)
        notificationHelper.sendReminderNotification(title, pills, soundUri)
    }
}