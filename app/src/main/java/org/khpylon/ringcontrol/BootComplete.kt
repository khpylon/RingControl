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
            if (action.equals(Intent.ACTION_MY_PACKAGE_REPLACED, ignoreCase = true) )
            {
                Storage(context).newInstall = true
            }
            if (Storage(context).isCalendarEnabled) {
                CalendarAlarmReceiver.startAlarm(context)
            }
            AudioChangeReceiver.nextAlarm(context)
        }
    }
}
