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

            // Update all widgets
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> onUpdate(context, manager, ids)

            // Change the ringer mode
            CHANGE_RINGER_MODE -> {
                val notificationManager =
                    context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                // If no permission, show a toast message
                if (!notificationManager.isNotificationPolicyAccessGranted) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.widget_needs_dnd_permissions),
                        Toast.LENGTH_SHORT
                    ).show()
                } else 
                
                // Otherwise cycle to the next mode and update all widgets
                {
                    val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
                    val currentMode = audioManager.getRingerMode()
                    val nextMode =
                        when (currentMode) {
                            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }

                    audioManager.ringerMode = nextMode
                    Storage(context).ringMode = nextMode
                    onUpdate(context, manager, ids)
                }
            }
        }
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, javaClass)
        intent.action = CHANGE_RINGER_MODE
        return PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun updateAppWidget(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget)
        val storage = Storage(context)

        // When the widget is clicked, generate an onUpdate broadcast
        views.setOnClickPendingIntent(
            R.id.logo,
            getPendingIntent(context)
        )

        // Draw the background of the widget
        var drawable = AppCompatResources.getDrawable(context, R.drawable.background) as Drawable
        views.setImageViewBitmap(R.id.background, drawBitmap(drawable, storage.backgroundColor))

        // Set color of text
        views.setTextColor(R.id.description, Color.White.toArgb())

        // Display text if visible
        views.setViewVisibility(
            R.id.description,
            if (storage.textVisible) View.VISIBLE else View.GONE
        )

        // Get the description for the widget's text
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = audioManager.getRingerMode()
        val description = if (storage.textDescription) {
            when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> context.getString(R.string.normal_description)
                AudioManager.RINGER_MODE_VIBRATE -> context.getString(R.string.vibrate_description)
                else -> context.getString(R.string.silent_description)
            }
        } else {
            context.getString(R.string.ringer_description)
        }
        views.setTextViewText(R.id.description, description)

        // Draw the foreground of the widget
        val symbol = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.outline_mobile_vibrate_48
            else -> R.drawable.outline_volume_off_48
        }
        drawable = AppCompatResources.getDrawable(context, symbol) as Drawable
        views.setImageViewBitmap(R.id.logo, drawBitmap(drawable, storage.foregroundColor))

        // Post the updates
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

        // Set the color on a widget component
        @JvmStatic
        fun drawBitmap(drawable: Drawable, color: Int): Bitmap {

            // Create a bitmap and canvas the same size as the drawable
            val bmp = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas = Canvas(bmp)

            // Create secondary bitmap and canvas
            val bmp2 = createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight)
            val canvas2 = Canvas(bmp2)

            // Fill with primary bitmap with the desired color
            val paint = Paint()
            paint.color = color
            paint.alpha = 0xff
            paint.style = Paint.Style.FILL
            canvas.drawPaint(paint)

            // Draw the image on the secondary canvas
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas2)

            // Merge the image and the color
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            canvas.drawBitmap(bmp2, 0f, 0f, paint)

            return bmp
        }
    }

}