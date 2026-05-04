package org.khpylon.ringcontrol

import java.time.LocalDateTime
import java.time.ZonedDateTime

class EventInfo(
    _beginTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    id: Int,
    val title: String,
    val isVibrate: Boolean,
    val isNotify: Boolean,
    _startOffset: Long,
    val isRepeating: Boolean
) {
    val eventId: Long = id.toLong()

    // Adjust beginning times to be one minute earlier
    val beginTime: ZonedDateTime = _beginTime

    val startOffset: Long = if (_startOffset < 15) _startOffset else 15

    private fun getOffset(): Long {
        return if (startOffset > 0) startOffset else 1
    }

    fun alarmTime(now: ZonedDateTime): LocalDateTime {
        val _beginTime = beginTime.minusMinutes(getOffset())
        return if (now.toInstant().toEpochMilli() < _beginTime.toInstant().toEpochMilli()) {
            _beginTime.toLocalDateTime()
        } else {
            endTime.toLocalDateTime()
        }
    }

    fun isEventActive(now: ZonedDateTime): Boolean {
        // current time is after begin time
        val afterBegin = now.toInstant().toEpochMilli() >= beginTime.minusMinutes(getOffset()).toInstant().toEpochMilli()
        // current time is before end time
        val beforeEnd = endTime.toInstant().toEpochMilli() > now.toInstant().toEpochMilli()
        return (afterBegin and beforeEnd)
    }
}
