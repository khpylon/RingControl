package org.khpylon.ringcontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootComplete : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action.equals(Intent.ACTION_BOOT_COMPLETED, ignoreCase = true) ||
            action.equals(Intent.ACTION_MY_PACKAGE_REPLACED, ignoreCase = true) )
        {

            // When a new version of the app is loaded
            if (action.equals(Intent.ACTION_MY_PACKAGE_REPLACED, ignoreCase = true) )
            {
                // Display the release notes when app is opened
                val storage = Storage(context)
                storage.newInstall = true
            }

            if (Storage(context).isCalendarEnabled) {
                CalendarAlarmReceiver.checkForEvents(context)
            }
            AudioChangeReceiver.nextAlarm(context)
        }
    }
}
