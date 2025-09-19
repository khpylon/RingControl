package org.khpylon.ringcontrol

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

private object StorageConstants {
    const val TAG = "storage"
    const val BG_COLOR = "bgColor"
    const val FG_COLOR = "fgColor"
    const val TEXT_VISIBLE = "textVisible"
    const val TEXT_DESCRIPTION = "textDescription"
}
class Storage(private val context: Context) {
    private fun commitWait(edit: SharedPreferences.Editor) {
        for (i in 0..9) {
            if (edit.commit()) {
                return
            }
        }
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

}