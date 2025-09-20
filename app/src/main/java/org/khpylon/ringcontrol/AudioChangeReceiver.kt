package org.khpylon.ringcontrol

import android.app.NotificationManager
import android.app.Service.NOTIFICATION_SERVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AudioChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notManager = context?.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val currentMode = notManager.currentInterruptionFilter
        val action = intent?.action
        when ( action ) {
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                when (currentMode) {
                    NotificationManager.INTERRUPTION_FILTER_ALL, // DND off
                    NotificationManager.INTERRUPTION_FILTER_PRIORITY // DND on
                        -> Widget.updateWidget(context)

                    NotificationManager.INTERRUPTION_FILTER_ALARMS,
                    NotificationManager.INTERRUPTION_FILTER_NONE,
                    NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> null
                }
            }
        }
    }
}
