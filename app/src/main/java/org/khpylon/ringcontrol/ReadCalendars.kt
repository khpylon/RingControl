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
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.collections.isNotEmpty

class ReadCalendars // Store context used locally.
internal constructor(private val mContext: Context) {

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

                // Ignore all-day events.
                if (!allDay && begin !== end) {

                    // If title or description contain the key phrase, process the event
                    val lowercaseTitle = title.lowercase()
                    if (lowercaseTitle.contains("#ringcontrol#") ||
                        lowercaseTitle.contains("#ring control#") ||
                        lowercaseTitle.contains("#rc#") ||
                        description.contains("#ringcontrol#") ||
                        description.contains("#ring control#") ||
                        description.contains("#rc#")
                    ) {
                        val endInMillis = end.toInstant().toEpochMilli()
                        if (endInMillis > lastEndTime) {
                            lastEndTime = endInMillis
                            events.add(
                                EventInfo(
                                    ZonedDateTime.ofInstant(begin.toInstant(), zoneId),
                                    ZonedDateTime.ofInstant(end.toInstant(), zoneId),
                                    eventId, title
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
                am.setRingerMode(AudioManager.RINGER_MODE_SILENT)
                // save event id
                appInfo.eventId = events[0].eventId
                // We are now handling an event.
                appInfo.appState = StorageConstants.ACTIVE

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

                val curRinger = am.ringerMode
                // If the ringer status has not been changed since we turned it off, restore it.
                if (curRinger == AudioManager.RINGER_MODE_SILENT) {
                    Log.d(Constants.LOGTAG, "findEvents() restoring ringer")
                    am.ringerMode = appInfo.ringStatus
                }
                appInfo.appState = StorageConstants.INACTIVE
                Log.d(Constants.LOGTAG, "findEvents() going INACTIVE")

            // Otherwise there's already another active event
            } else {
                // save new event id
                appInfo.appState = StorageConstants.ACTIVE

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
