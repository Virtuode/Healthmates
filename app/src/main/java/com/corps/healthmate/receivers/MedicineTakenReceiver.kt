package com.corps.healthmate.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class MedicineTakenReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)

        // Cancel the notification
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        // Stop the ringtone
        val stopRingtoneIntent = Intent(context, AlarmReceiver::class.java)
        stopRingtoneIntent.setAction("STOP_RINGTONE")
        context.sendBroadcast(stopRingtoneIntent)

        Timber.tag(TAG)
            .d("Medicine taken action received for notification: %s", notificationId)
    }

    companion object {
        private const val TAG = "MedicineTakenReceiver"
    }
}