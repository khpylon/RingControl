package org.khpylon.ringcontrol

import android.app.NotificationManager
import android.app.Service.NOTIFICATION_SERVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AudioChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notManager = context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val currentMode = notManager.currentInterruptionFilter
        val action = intent?.action
        when ( action ) {
            android.media.AudioManager.RINGER_MODE_CHANGED_ACTION ->  "2"
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> "1"
        }
        Log.d("943TSX","AudioChangeReceiver received $action")
        when (currentMode) {
            // DND off
            NotificationManager.INTERRUPTION_FILTER_ALL,
            // DND on
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
                -> Widget.updateWidget(context)
////            NotificationManager.INTERRUPTION_FILTER_NONE -> 0
////            NotificationManager.INTERRUPTION_FILTER_ALARMS -> 0
////            NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> 0
        }
    }
}
