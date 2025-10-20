package org.khpylon.ringcontrol

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object StorageConstants {
    const val TAG = "storage"
    const val RING_MODE = "ringMode"
    const val BG_COLOR = "bgColor"
    const val FG_COLOR = "fgColor"
    const val TEXT_VISIBLE = "textVisible"
    const val TEXT_DESCRIPTION = "textDescription"
    const val WIDGET_SCALE = "widgetScale"
    const val NEW_INSTALL = "newInstall"
    const val EVENT_ID: String = "event_id"
    const val APP_STATE: String = "app_state"
    const val RING_STATUS: String = "ring_status"
    const val CALENDAR_STATUS: String = "calendar_status"
    const val INACTIVE: Int = 0
    const val SILENT: Int = 1
    const val VIBRATE: Int = 2

}

class Storage(private val context: Context) {
    private fun commitWait(edit: SharedPreferences.Editor) {
        for (i in 0..9) {
            if (edit.commit()) {
                return
            }
        }
    }

    var ringMode: Int
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getInt(StorageConstants.RING_MODE, AudioManager.RINGER_MODE_NORMAL)
        }
        set(mode) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putInt(StorageConstants.RING_MODE, mode)
            commitWait(edit)
        }

    var backgroundColor: Int
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getInt(StorageConstants.BG_COLOR, Color.Green.toArgb()) or (0xff shl 24)
        }
        set(color) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putInt(StorageConstants.BG_COLOR, color or (0xff shl 24))
            commitWait(edit)
        }

    var foregroundColor: Int
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getInt(StorageConstants.FG_COLOR, Color.Black.toArgb())
        }
        set(color) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putInt(StorageConstants.FG_COLOR, color or (0xff shl 24))
            commitWait(edit)
        }

    var textVisible: Boolean
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getBoolean(StorageConstants.TEXT_VISIBLE, true)
        }
        set(visibility) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putBoolean(StorageConstants.TEXT_VISIBLE, visibility)
            commitWait(edit)
        }

    var textDescription: Boolean
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getBoolean(StorageConstants.TEXT_DESCRIPTION, true)
        }
        set(descriptive) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putBoolean(StorageConstants.TEXT_DESCRIPTION, descriptive)
            commitWait(edit)
        }

    var widgetScale: Float
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getFloat(StorageConstants.WIDGET_SCALE, 1f)
        }
        set(scale) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putFloat(StorageConstants.WIDGET_SCALE, scale)
            commitWait(edit)
        }


    var newInstall: Boolean
        get() {
            val pref = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE)
            return pref.getBoolean(StorageConstants.NEW_INSTALL, true)
        }
        set(isNewInstall) {
            val edit = context.getSharedPreferences(StorageConstants.TAG, Context.MODE_PRIVATE).edit()
            edit.putBoolean(StorageConstants.NEW_INSTALL, isNewInstall)
            commitWait(edit)
        }

    var eventId: Long
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getLong(StorageConstants.EVENT_ID, 0)
        }
        set(id) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putLong(StorageConstants.EVENT_ID, id)
            // Commit the changes
            commitWait(edit)
        }

    var appState: Int
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getInt(StorageConstants.APP_STATE, StorageConstants.INACTIVE)
        }
        set(id) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putInt(StorageConstants.APP_STATE, id)
            // Commit the changes
            commitWait(edit)
        }

    var ringStatus: Int
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getInt(StorageConstants.RING_STATUS, 0)
        }
        set(id) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putInt(StorageConstants.RING_STATUS, id)
            // Commit the changes
            commitWait(edit)
        }

    var isCalendarEnabled: Boolean
        get() {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            return pref.getBoolean(StorageConstants.CALENDAR_STATUS, false)
        }
        set(value) {
            val pref = context.getSharedPreferences(
                StorageConstants.TAG,
                Context.MODE_PRIVATE
            )
            val edit = pref.edit()
            // Store data. you may also use putFloat(), putInt(), putLong() as requirement
            edit.putBoolean(StorageConstants.CALENDAR_STATUS, value)
            // Commit the changes
            commitWait(edit)
        }

}