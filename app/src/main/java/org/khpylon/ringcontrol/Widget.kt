package org.khpylon.ringcontrol

import android.app.Activity.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service.AUDIO_SERVICE
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.widget.RemoteViews
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb


open class Widget : AppWidgetProvider() {
    private val CHANGE_RINGER_MODE = "TOUCH"

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, this.javaClass))
        when (intent.action) {
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> onUpdate(context, manager, ids)
            CHANGE_RINGER_MODE -> {
                val notificationManager =
                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                // the below code is to check the permission that the access notification policy settings from users device..
                if (!notificationManager.isNotificationPolicyAccessGranted) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.widget_needs_dnd_permissions),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                    val currentMode = audioManager.getRingerMode()

                    audioManager.setRingerMode(
                        when (currentMode) {
                            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }
                    )
                    onUpdate(context, manager, ids)
//                    Toast.makeText(
//                        context,
//                        when (currentMode) {
//                            AudioManager.RINGER_MODE_NORMAL -> "Vibrate"
//                            AudioManager.RINGER_MODE_VIBRATE -> "Silent"
//                            else -> "Normal"
//                        },
//                        Toast.LENGTH_SHORT
//                    ).show()
                }
            }
        }
    }

    private fun getPendingIntent(context: Context, value: Int): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = CHANGE_RINGER_MODE
        return PendingIntent.getBroadcast(
            context, value, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun updateAppWidget(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget)

        views.setOnClickPendingIntent(
            R.id.logo,
            getPendingIntent(context, 0)
        )

//        val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.outline_volume_up_48_black)

        // Set color of text
        views.setTextColor(R.id.description, Color.White.toArgb())

        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.getRingerMode()

        val symbol = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48_black
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.outline_mobile_vibrate_48
            else -> R.drawable.outline_volume_off_48_black
        }

        views.setImageViewResource(R.id.logo, symbol)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        @JvmStatic
        fun updateWidget(context: Context) {
            val updateIntent = Intent()
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            context.sendBroadcast(updateIntent)
        }
    }

}