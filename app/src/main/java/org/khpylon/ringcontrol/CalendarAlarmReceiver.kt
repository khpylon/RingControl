package org.khpylon.ringcontrol

import android.app.AlarmManager
//import android.app.NotificationChannel
//import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
//import androidx.core.app.NotificationCompat
//import androidx.core.content.ContextCompat.getSystemService
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

private const val INTERVAL = 20L  // if no events are found, check again in 20 minutes

class CalendarAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val time = LocalDateTime.now(ZoneId.systemDefault())

        val events = ReadCalendars(context).findEvents(time)
        val isCalendarEvent = !events.isEmpty()

        val next = if (!isCalendarEvent) {
            time.plusMinutes(INTERVAL)
        } else {
            events[0].alarmTime(time.atZone(ZoneId.systemDefault()))
        }

        val timeText = next.format(
            java.time.format.DateTimeFormatter.ofPattern(
                "MM/dd HH:mm:ss",
                java.util.Locale.US
            )
        )

        android.util.Log.d(Constants.LOGTAG, "Next AlarmReceiver at $timeText")
        android.util.Log.d(Constants.LOGTAG, "isCalendarEvent is $isCalendarEvent")

//        val notificationManager =
//            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        if (notificationManager.getNotificationChannel(NORMAL_NOTIFICATIONS) == null) {
//            createNotificationChannels(context)
//        }
//
//        // If an event is active, or the user wants to see the app is active, display an notification
//        if (events.isNotEmpty() && events[0].isEventActive(time.atZone(ZoneId.systemDefault()))) {
//            val title = events[0].title
//            android.util.Log.e(Constants.LOGTAG, "event is '$title'")
//            val notificationManager =
//                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//            val builder = NotificationCompat.Builder(context, NORMAL_NOTIFICATIONS)
//                .setSmallIcon(R.drawable.ic_launcher_foreground)
//                .setContentTitle("Read Calendar")
//                .setContentText("Next AlarmReceiver at $timeText for '$title'")
//                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//            notificationManager.notify(7, builder.build())
//        }

        val nextAlarmAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, CalendarAlarmReceiver::class.java)
        val pendingIntent =
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager


//        if (isCalendarEvent && alarmManager.canScheduleExactAlarms()) {
//            alarmManager.setExact(
//                AlarmManager.RTC_WAKEUP,
//                nextAlarmAt,
//                pendingIntent
//            )
//        } else {
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            nextAlarmAt,
            DateUtils.SECOND_IN_MILLIS * 5,
            pendingIntent
        )
//        }
    }

    companion object {
//        val NORMAL_NOTIFICATIONS = "NORMAL_NOTIFICATIONS"
//
//        @JvmStatic
//        fun createNotificationChannels(context: Context) {
//
//            val notificationManager: NotificationManager = getSystemService<NotificationManager>(
//                context,
//                NotificationManager::class.java
//            ) as NotificationManager
//
//            // Create the NotificationChannel, but only on API 26+ because
//            // the NotificationChannel class is new and not in the support library
//            var name: CharSequence = "Informational" // getString(R.string.channel_name);
//            var importance = NotificationManager.IMPORTANCE_DEFAULT
//            var channel = NotificationChannel(NORMAL_NOTIFICATIONS, name, importance)
//            channel.description =
//                "General information from the app" // getString(R.string.channel_description);
//            // Register the channel with the system; you can't change the importance
//            // or other notification behaviors after this
//            notificationManager.createNotificationChannel(channel)
//        }

        @JvmStatic
        fun startAlarm (context: Context) {
            val intent = Intent(context, CalendarAlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                Date().time + 5 * DateUtils.SECOND_IN_MILLIS,
                DateUtils.SECOND_IN_MILLIS,
                pendingIntent
            )
            android.util.Log.d(Constants.LOGTAG, "starting alarm")
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            val intent = Intent(context, CalendarAlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            android.util.Log.d(Constants.LOGTAG, "canceling alarm")
        }

    }
}
