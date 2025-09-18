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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap

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
        val storage = Storage(context)

        views.setOnClickPendingIntent(
            R.id.logo,
            getPendingIntent(context, 0)
        )

        val drawable = AppCompatResources.getDrawable(context, R.drawable.background) as Drawable
        views.setImageViewBitmap(R.id.background, drawBitmap(drawable, storage.backgroundColor))

        // Set color of text
        views.setTextColor(R.id.description, Color.White.toArgb())

        // Display text if visible
        views.setViewVisibility(R.id.description, if (storage.textVisible) View.VISIBLE else View.GONE)

        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.getRingerMode()

        val description = if (storage.textDescription) {
            when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> "Normal"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                else -> "Silent"
            }
        } else {
            "Ringer"
        }
        views.setTextViewText(R.id.description, description)

        val symbol = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.outline_mobile_vibrate_48
            else -> R.drawable.outline_volume_off_48
        }

        val drawable2 = AppCompatResources.getDrawable(context, symbol) as Drawable
        views.setImageViewBitmap(R.id.logo, drawBitmap(drawable2, storage.foregroundColor))

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

        @JvmStatic
        fun drawBitmap(drawable: Drawable, color: Int): Bitmap {
            val bmp = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)

            val canvas = Canvas(bmp)

            val bmp2 = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas2 = Canvas(bmp2)

            val paint = Paint()

            // Fill with the primary color mask
            paint.color = color
            paint.alpha = 0xff
            paint.style = Paint.Style.FILL
            canvas.drawPaint(paint)

            // Draw the image
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas2)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            canvas.drawBitmap(bmp2, 0f, 0f, paint)

            return bmp
        }
    }

}