package org.khpylon.ringcontrol

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
//import android.app.Service.NOTIFICATION_SERVICE
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.media.AudioManager
import android.text.format.DateUtils
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId

class AudioChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val action = intent?.action
        when ( action ) {
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                if (Storage(context).ringMode != audioManager.ringerMode) {
                    Storage(context).ringMode = audioManager.ringerMode
                    Log.d(Constants.LOGTAG, "widget update needed")
                    Widget.updateWidget(context)
                }
//                val notManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//                val currentMode = notManager.currentInterruptionFilter
//                when (currentMode) {
//                    NotificationManager.INTERRUPTION_FILTER_ALL, // DND off
//                    NotificationManager.INTERRUPTION_FILTER_PRIORITY // DND on
//                        -> Widget.updateWidget(context)
//
//                    NotificationManager.INTERRUPTION_FILTER_ALARMS,
//                    NotificationManager.INTERRUPTION_FILTER_NONE,
//                    NotificationManager.INTERRUPTION_FILTER_UNKNOWN -> null
//                }
            }
            else -> {
                val stored = Storage(context).ringMode
                val mode = audioManager.ringerMode
                Log.d(Constants.LOGTAG, "alarm stored=$stored mode=$mode")
                if (stored != mode) {
                    Storage(context).ringMode = audioManager.ringerMode
                    Log.d(Constants.LOGTAG, "widget update needed")
                    Widget.updateWidget(context)
                }
                nextAlarm(context)
            }
        }
    }

    companion object {
        private fun getIntent(context: Context): Intent {
            return Intent(context, AudioChangeReceiver::class.java).setAction("Alarm")
        }

        @JvmStatic
        fun nextAlarm(context: Context) {
            val delay = 20
            val time = LocalDateTime.now(ZoneId.systemDefault()).plusSeconds(delay.toLong())
            val nextTime = time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val pendingIntent = PendingIntent.getBroadcast(
                context, 0,
                getIntent(context), PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager[AlarmManager.RTC_WAKEUP, nextTime] = pendingIntent
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                nextTime,
                DateUtils.SECOND_IN_MILLIS * 5,
                pendingIntent
            )
        }
    }
}
