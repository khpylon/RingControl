package org.khpylon.ringcontrol

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.PowerManager
import android.text.format.DateUtils
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

object CalendarConstants {
    const val INTERVAL = 20L  // if no events are found, check again in 20 minutes
}

class CalendarAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        checkForEvents(context)
    }

    companion object {
        private const val NORMAL_NOTIFICATIONS = "NORMAL_NOTIFICATIONS"

        @JvmStatic
        fun checkForEvents(context: Context) {
            val storage = Storage(context)

            // If the calendar access is disabled, don't do anything
            if (context.checkSelfPermission(
                    Manifest.permission.READ_CALENDAR
                ) != PackageManager.PERMISSION_GRANTED ||
                !storage.isCalendarEnabled) {
                Log.d(Constants.LOGTAG, "Bypassing calendar checks due to calendar restrictions.")
                return
            }

            // If battery optimizations are enabled, we can't change the ringer mode.
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                Log.d(Constants.LOGTAG, "Bypassing calendar checks due to battery restrictions.")
                return
            }

            val time = LocalDateTime.now(ZoneId.systemDefault())

            // Check to see if there are pending events
            val events = ReadCalendars(context).findEvents(time)
            val isCalendarEvent = !events.isEmpty()

            // Determine when the next alarm should occur
            val next = if (!isCalendarEvent) {
                time.plusMinutes(CalendarConstants.INTERVAL)
            } else {
                events[0].alarmTime(time.atZone(ZoneId.systemDefault()))
            }

            storage.isPending = isCalendarEvent

            // Format the time for the next alarm.
            val dateFormatter =
                DateTimeFormatter.ofLocalizedDate(
                    FormatStyle.SHORT
                ).withLocale(Locale.getDefault())
            val dateText = time.format(dateFormatter)
            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val timeFormatter =
                DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
            val timeText = time.format(timeFormatter)
            Log.d(Constants.LOGTAG, "This AlarmReceiver at $dateText $timeText")
            Log.d(Constants.LOGTAG, "isCalendarEvent is $isCalendarEvent")

            // If an event is active, or the user wants to see the app is active, display a notification
            if (storage.isNotificationEnabled && events.isNotEmpty()
                && events[0].isEventActive(time.atZone(ZoneId.systemDefault()))
                && events[0].isNotify
            ) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (notificationManager.getNotificationChannel(NORMAL_NOTIFICATIONS) == null) {
                    createNotificationChannels(context)
                }

                val title = events[0].title
                Log.d(Constants.LOGTAG, "event is '$title'")
                val builder = NotificationCompat.Builder(context, NORMAL_NOTIFICATIONS)
                    .setSmallIcon(R.drawable.notifier_recycler)
                    .setContentTitle("Ring Control")
                    .setContentText("Ringer disabled at $dateText $timeText for calendar event \"$title\"")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                notificationManager.notify(7, builder.build())
            }

            // Set the next alarm
            setAlarm(context, next)
        }

        @JvmStatic
        fun createNotificationChannels(context: Context) {
            val notificationManager: NotificationManager = getSystemService(
                context,
                NotificationManager::class.java
            ) as NotificationManager

            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name: CharSequence = context.getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NORMAL_NOTIFICATIONS, name, importance)
            channel.description =
                context.getString(R.string.channel_description)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel)
        }

        @JvmStatic
        fun setAlarm(context: Context, next: LocalDateTime) {

            // If battery optimizations are enabled, we can't change the ringer mode.
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                Log.d(Constants.LOGTAG, "Bypassing calendar alarms due to battery restrictions.")
                return
            }

            val dateFormatter = DateTimeFormatter.ofLocalizedDate(
                FormatStyle.SHORT
            ).withLocale(Locale.getDefault())
            val dateText = next.format(dateFormatter)

            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            val timeFormatter =
                DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a")
            val timeText = next.format(timeFormatter)
            Log.d(Constants.LOGTAG, "Next AlarmReceiver at $dateText $timeText")

            val alarmTime = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val intent = Intent(context, CalendarAlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                DateUtils.SECOND_IN_MILLIS * 5,
                pendingIntent
            )
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            val intent = Intent(context, CalendarAlarmReceiver::class.java)
            val pendingIntent =
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(Constants.LOGTAG, "canceling alarm")
        }

    }
}
