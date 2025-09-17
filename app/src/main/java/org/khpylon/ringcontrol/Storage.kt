package org.khpylon.ringcontrol

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class Storage(val context: Context) {
    private fun commitWait(edit: SharedPreferences.Editor) {
        for (i in 0..9) {
            if (edit.commit()) {
                return
            }
        }
    }

    private val TAG = "storage"
    var backgroundColor: Int
        get() {
            val pref = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            return pref.getInt("bgColor", Color.Green.toArgb()) or (0xff shl 24)
        }
        set(color) {
            val edit = context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
            edit.putInt("bgColor", color or (0xff shl 24))
            commitWait(edit)
        }

    var foregroundColor: Int
        get() {
            val pref = context.getSharedPreferences(TAG, Context.MODE_PRIVATE)
            return pref.getInt("fgColor", Color.Black.toArgb())
        }
        set(color) {
            val edit = context.getSharedPreferences(TAG, Context.MODE_PRIVATE).edit()
            edit.putInt("fgColor", color or (0xff shl 24))
            commitWait(edit)
        }

}