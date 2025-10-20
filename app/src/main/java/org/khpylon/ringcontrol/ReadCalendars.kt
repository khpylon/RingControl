package org.khpylon.ringcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.AudioManager
import android.provider.CalendarContract
import android.text.format.DateUtils
import android.util.Log
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import androidx.core.net.toUri
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.collections.isNotEmpty

private const val NO_ANNOTATION = 0
private const val DND_ANNOTATION = 1
private const val VIBRATE_ANNOTATION = 2

class ReadCalendars // Store context used locally.
internal constructor(private val mContext: Context) {

    private fun checkForAnnotations(string: String): Int {
        val lowercaseString = string.lowercase()

        return if (lowercaseString.contains("#ringcontrol/v#") ||
            lowercaseString.contains("#ring control/v#") ||
            lowercaseString.contains("#rc/v#")
        ) {
            VIBRATE_ANNOTATION
        } else if (lowercaseString.contains("#ringcontrol#") ||
            lowercaseString.contains("#ring control#") ||
            lowercaseString.contains("#rc#")
        ) {
            DND_ANNOTATION
        } else {
            NO_ANNOTATION
        }
    }

    // Find all calendar events within a certain interval.
    @SuppressLint("Range")
    fun readCalendar(time: LocalDateTime, window: Long): MutableList<EventInfo> {
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
                val description =
                    eventCursor.getString(eventCursor.getColumnIndex(CalendarContract.EventsEntity.DESCRIPTION))
                        .lowercase()

                // Ignore all-day events and events longer than a day.
                if (!allDay && begin !== end &&
                    ChronoUnit.HOURS.between(begin.toInstant(), end.toInstant()) < 24
                ) {

                    val titleMatch = checkForAnnotations(title)
                    val descriptionMatch = checkForAnnotations(description)
                    // If title or description contain the key phrase, process the event

                    if (titleMatch != NO_ANNOTATION || descriptionMatch != NO_ANNOTATION) {
                        val endInMillis = end.toInstant().toEpochMilli()
                        val isVibrate = titleMatch == VIBRATE_ANNOTATION
                                || descriptionMatch == VIBRATE_ANNOTATION
                        if (endInMillis > lastEndTime) {
                            lastEndTime = endInMillis
                            events.add(
                                EventInfo(
                                    ZonedDateTime.ofInstant(begin.toInstant(), zoneId),
                                    ZonedDateTime.ofInstant(end.toInstant(), zoneId),
                                    eventId, title, isVibrate
                                )
                            )
                            Log.e(Constants.LOGTAG, "Found event '$title' ($eventId)")
                        }
                        if (events.size == 2) {
                            Log.e(Constants.LOGTAG, "Skipping additional events.")
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

        // If we still don't have permissions to access calendars or ringer, punt.
        if (mContext.checkSelfPermission(
                Manifest.permission.READ_CALENDAR
            ) != PackageManager.PERMISSION_GRANTED ||
            mContext.checkSelfPermission(
                Manifest.permission.ACCESS_NOTIFICATION_POLICY
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return mutableListOf()
        }

        // Prepare to read application settings.
        val appInfo = Storage(mContext)

        // Check calendars for events in the next 30 minutes.
        val events = readCalendar(now, 30L)

        // If we ARE NOT currently handling an event.....
        if (appInfo.appState == StorageConstants.INACTIVE) {
            // If the first event is happening, become active and silence ringer
            if (events.isNotEmpty() && events[0].isEventActive(now.atZone(ZoneId.systemDefault()))) {
                val am: AudioManager =
                    mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                // save current ringer setting
                appInfo.ringStatus = am.getRingerMode()
                // silence the ringer
                am.setRingerMode(if (events[0].isVibrate) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_SILENT)
                // save event id
                appInfo.eventId = events[0].eventId
                // We are now handling an event.
                appInfo.appState = if (events[0].isVibrate) StorageConstants.VIBRATE else StorageConstants.SILENT

                val endTime = events[0].endTime
                val title = events[0].title
                Log.d(
                    Constants.LOGTAG,
                    "findEvents() going ACTIVE until $endTime for '$title'"
                )
            }

            // If we ARE currently handling an event.....
        } else {
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

            // If first event in the list is the active event, get rid of it.
            if (events.isNotEmpty() && appInfo.eventId == events[0].eventId) {
                events.removeAt(0)
            }

            // If there are no other events, or if the next event isn't active, restore settings and
            // become inactive again.
            if (events.isEmpty() || !events[0]
                    .isEventActive(now.atZone(ZoneId.systemDefault()))
            ) {
                val am: AudioManager =
                    mContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                // If the ringer status has not been changed since we modified it, restore it.
                val curRinger = am.ringerMode
                val appState = appInfo.appState
                if ((appState == StorageConstants.SILENT && curRinger == AudioManager.RINGER_MODE_SILENT)
                    || (appState == StorageConstants.VIBRATE && curRinger == AudioManager.RINGER_MODE_VIBRATE)) {
                    Log.d(Constants.LOGTAG, "findEvents() restoring ringer")
                    am.ringerMode = appInfo.ringStatus
                }
                appInfo.appState = StorageConstants.INACTIVE
                Log.d(Constants.LOGTAG, "findEvents() going INACTIVE")

                // Otherwise there's already another active event
            } else {
                // save new event id
                appInfo.appState = StorageConstants.SILENT

                val endTime = events[0].endTime
                val title = events[0].title
                Log.d(
                    Constants.LOGTAG,
                    "findEvents() staying ACTIVE until $endTime for '$title'"
                )
            }
        }
        return events
    }
}
