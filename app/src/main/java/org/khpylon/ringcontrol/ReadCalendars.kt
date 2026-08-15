package org.khpylon.ringcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.os.PowerManager
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.util.Log
import androidx.core.net.toUri
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.regex.Pattern

private const val NO_ANNOTATION = 0
private const val DND_ANNOTATION = 1
private const val VIBRATE_ANNOTATION = 2
private const val NOTIFY_ANNOTATION = 3
private const val VIBRATE_NOTIFY_ANNOTATION = 4

class ReadCalendars // Store context used locally.
internal constructor(private val mContext: Context) {

    private fun checkForAnnotations(string: String): Int {
        val pattern = Pattern.compile("#(rc|ring ?control)([^#]*)#", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(string)
        return if (matcher.find()) {
            if (matcher.groupCount() == 1) {
                return DND_ANNOTATION
            } else {
                val substring = matcher.group(2)!!.lowercase()
                return if (substring.contains("n") && substring.contains("v")) {
                    VIBRATE_NOTIFY_ANNOTATION
                } else if (substring.contains("v")) {
                    VIBRATE_ANNOTATION
                } else if (substring.contains("n")) {
                    NOTIFY_ANNOTATION
                } else {
                    DND_ANNOTATION
                }
            }
        } else {
            NO_ANNOTATION
        }
    }

    private fun delayAnnotation(string: String): Long {
        val pattern = Pattern.compile("#(rc|ring ?control)([^#]*)#", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(string)
        if (matcher.find() && matcher.groupCount() == 2) {
            val substring = matcher.group(2)!!.lowercase()
            val timePattern = Pattern.compile("""\+(\d+)""", Pattern.CASE_INSENSITIVE)
            val timeMatcher = timePattern.matcher(substring)
            if (timeMatcher.find()) {
                return timeMatcher.group().substring(1).toLong()
            }
        }
        return 0L
    }

    private fun isVibrate(annotation: Int): Boolean {
        return (annotation == VIBRATE_ANNOTATION) || (annotation == VIBRATE_NOTIFY_ANNOTATION)
    }

    private fun isNotify(annotation: Int): Boolean {
        return (annotation == NOTIFY_ANNOTATION) || (annotation == VIBRATE_NOTIFY_ANNOTATION)
    }

    // Find all calendar events within a certain interval.
    @SuppressLint("Range")
    fun readCalendar(time: LocalDateTime, window: Long, numEvents: Int): MutableList<EventInfo> {

        // Prepare to read application settings.
        val appInfo = Storage(mContext)

        // If we still don't have permissions to access calendars or ringer, punt.
        if (mContext.checkSelfPermission(
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED ||
            mContext.checkSelfPermission(
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            ) != PackageManager.PERMISSION_GRANTED ||
            !appInfo.isCalendarEnabled
        ) {
            return mutableListOf()
        }

        val events = mutableListOf<EventInfo>()
        val zoneId = ZoneId.systemDefault()

        // Convert current "time" to date format needed in calendar query.
        val now = time.atZone(zoneId).toInstant().toEpochMilli()

        val builder = CalendarContract.Instances.CONTENT_URI.toString().toUri().buildUpon()

        // search for instances over a timespan starting 2 minutes earlier
        ContentUris.appendId(builder, now - 2 * DateUtils.MINUTE_IN_MILLIS)
        ContentUris.appendId(builder, now + window * DateUtils.MINUTE_IN_MILLIS)

        val contentResolver: ContentResolver = mContext.contentResolver

        // Get a ton of info from each event instance.
        val eventCursor: Cursor? = contentResolver.query(
            builder.build(),
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.RRULE,
                CalendarContract.EventsEntity.DESCRIPTION,
            ), null, null, "begin ASC"
        )

        // If any events are found, process them.
        if (eventCursor!!.count > 0) {
            var lastEndTime = now
            eventCursor.moveToFirst()
            do {
                val allDay =
                    eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)) != "0"
                val begin =
                    Date(eventCursor.getLong(eventCursor.getColumnIndex(CalendarContract.Instances.BEGIN)))
                val end =
                    Date(eventCursor.getLong(eventCursor.getColumnIndex(CalendarContract.Instances.END)))
                val eventId =
                    eventCursor.getInt(eventCursor.getColumnIndex(CalendarContract.Instances.EVENT_ID))
                val title =
                    eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Instances.TITLE))
                val rrule =
                    eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.Instances.RRULE))
                val description =
                    eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.EventsEntity.DESCRIPTION))
                        .lowercase()

//                val rule = if (rrule == null) RRule()
//                    else RRule("RRULE:$rrule")

                // Ignore all-day events and events longer than a day.
                if (!allDay && begin !== end &&
                    ChronoUnit.HOURS.between(begin.toInstant(), end.toInstant()) < 24
                ) {

                    val titleMatch = checkForAnnotations(title)
                    val descriptionMatch = checkForAnnotations(description)
                    // If title or description contain the key phrase, process the event

                    if (titleMatch != NO_ANNOTATION || descriptionMatch != NO_ANNOTATION) {
                        var startOffset = delayAnnotation(title)
                        if (startOffset == 0L) {
                            startOffset = delayAnnotation(description)
                        }
                        val endInMillis = end.toInstant().toEpochMilli()
                        val isVibrate = isVibrate(titleMatch)
                                || isVibrate(descriptionMatch)
                        val isNotify = isNotify(titleMatch)
                                || isNotify(descriptionMatch)
                        if (endInMillis > lastEndTime) {
                            lastEndTime = endInMillis
                            events.add(
                                EventInfo(
                                    ZonedDateTime.ofInstant(begin.toInstant(), zoneId),
                                    ZonedDateTime.ofInstant(end.toInstant(), zoneId),
                                    eventId, title, isVibrate, isNotify,
                                    startOffset,
                                    rrule != null && rrule.isNotEmpty()
                                )
                            )
                            Log.d(Constants.LOGTAG, "Found event '$title' ($eventId)")
                        }
                        if (events.size == numEvents) {
                            Log.d(Constants.LOGTAG, "Skipping additional events.")
                            break
                        }
                    }
                }
            } while (eventCursor.moveToNext())
            eventCursor.close()
        }
        return events
    }

    // Logic for controlling the ringer.
    fun findEvents(now: LocalDateTime): MutableList<EventInfo> {

        // Prepare to read application settings.
        val appInfo = Storage(mContext)

        // Check calendars for events in the next 30 minutes.
        val events = readCalendar(now, 30L, 2)

        // If we ARE NOT currently handling an event.....
        if (appInfo.appState == StorageConstants.INACTIVE) {
            // If the first event is happening, become active and silence ringer
            if (events.isNotEmpty() && events[0].isEventActive(now.atZone(ZoneId.systemDefault()))) {
                val am: AudioManager =
                    mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                // save current ringer setting
                appInfo.ringStatus = am.ringerMode

                // save event id
                appInfo.eventId = events[0].eventId

                // set the app state
                appInfo.appState =
                    if (events[0].isVibrate) StorageConstants.VIBRATE else StorageConstants.SILENT

                // modify the ringer and remember its value
                val ringerMode =
                    if (events[0].isVibrate) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_SILENT
                changeRinger(ringerMode)

                val endTime = events[0].endTime
                val title = events[0].title
                Log.d(
                    Constants.LOGTAG,
                    "findEvents() going ACTIVE until $endTime for '$title'"
                )
            }

            // If we ARE currently handling an event.....
        } else {

            // Get rid of them all but the first two events
            if (events.isNotEmpty()) {
                while (events.size > 2) {
                    val title = events[events.lastIndex].title
                    Log.d(
                        Constants.LOGTAG,
                        "findEvents() removing '$title'"
                    )
                    events.removeAt(events.lastIndex)
                }
            }

            // If first event in the list is the current event and it's not active,
            // discard it.
            if (events.isNotEmpty() && appInfo.eventId == events[0].eventId
                && !events[0].isEventActive(now.atZone(ZoneId.systemDefault()))
            ) {
                events.removeAt(0)
            }

            // If there are no other events, or if the next event isn't active, restore settings and
            // become inactive again.
            if (events.isEmpty() ||
                !events[0].isEventActive(now.atZone(ZoneId.systemDefault()))
            ) {
                val am: AudioManager =
                    mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                // If the ringer status has not been changed since we modified it, restore it.
                val curRinger = am.ringerMode
                val appState = appInfo.appState
                if ((appState == StorageConstants.SILENT && curRinger == AudioManager.RINGER_MODE_SILENT)
                    || (appState == StorageConstants.VIBRATE && curRinger == AudioManager.RINGER_MODE_VIBRATE)
                ) {
                    changeRinger(appInfo.ringStatus)
                }

                appInfo.appState = StorageConstants.INACTIVE
                Log.d(Constants.LOGTAG, "findEvents() going INACTIVE")

                // Otherwise there's already an active event
            } else {
                val title = events[0].title
                if (appInfo.eventId == events[0].eventId) {
                    Log.d(
                        Constants.LOGTAG,
                        "findEvents() '$title' already ACTIVE"
                    )
                } else {
                    // save event id
                    appInfo.eventId = events[0].eventId

                    // set the app state
                    appInfo.appState =
                        if (events[0].isVibrate) StorageConstants.VIBRATE else StorageConstants.SILENT

                    // modify the ringer and remember its value
                    val ringerMode =
                        if (events[0].isVibrate) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_SILENT
                    changeRinger(ringerMode)

                    val endTime = events[0].endTime
                    Log.d(
                        Constants.LOGTAG,
                        "findEvents() staying ACTIVE until $endTime for '$title'"
                    )
                }
            }
        }
        return events
    }

    // Change the ringer mode
    private fun changeRinger(mode: Int) {
        val serviceIntent = Intent(mContext, RingerModeService::class.java).apply {
            putExtra("MODE", mode)
        }

        // Run service to change ringer mode
        mContext.startForegroundService(serviceIntent)
        Log.d(
            Constants.LOGTAG, "changeRinger() setting ringer to " +
                    when (mode) {
                        AudioManager.RINGER_MODE_NORMAL -> "normal"
                        AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
                        AudioManager.RINGER_MODE_SILENT -> "silent"
                        else -> "unknown!"
                    }
        )
    }

}
