package org.khpylon.ringcontrol

import java.time.LocalDateTime
import java.time.ZonedDateTime

class EventInfo(
    _beginTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    id: Int,
    val title: String
) {
    val eventId: Long = id.toLong()

    // Adjust beginning times to be one minute earlier
    val beginTime: ZonedDateTime = _beginTime.minusMinutes(1)

    fun alarmTime(now: ZonedDateTime): LocalDateTime {
        return if (now.toInstant().toEpochMilli() < beginTime.toInstant().toEpochMilli()) {
            beginTime.toLocalDateTime()
        } else {
            endTime.toLocalDateTime()
        }
    }

    fun isEventActive(now: ZonedDateTime): Boolean {
        // current time is after begin time
        val afterBegin = now.toInstant().toEpochMilli() >= beginTime.toInstant().toEpochMilli()
        // current time is before end time
        val beforeEnd = endTime.toInstant().toEpochMilli() > now.toInstant().toEpochMilli()
        return (afterBegin and beforeEnd)
    }
}
