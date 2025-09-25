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
                    val currentMode = getRingerMode(audioManager)
                    val nextMode =
                        when (currentMode) {
                            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }

                    audioManager.ringerMode = setRingerMode(audioManager, nextMode)
                    Storage(context).ringMode = setRingerMode(audioManager, nextMode)
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
        views.setImageViewBitmap(
            R.id.background,
            drawBitmap(drawable, storage.backgroundColor, storage.widgetScale)
        )

        // Get the description for the widget's text
        val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
        val currentMode = getRingerMode(audioManager)
        val description = if (!storage.textVisible) "" else
            if (storage.textDescription) {
                when (currentMode) {
                    AudioManager.RINGER_MODE_NORMAL -> context.getString(R.string.normal_description)
                    AudioManager.RINGER_MODE_VIBRATE -> context.getString(R.string.vibrate_description)
                    else -> context.getString(R.string.silent_description)
                }
            } else {
                context.getString(R.string.ringer_description)
            }

        views.setImageViewBitmap(R.id.text, drawTextBitmap(description, storage.widgetScale))

        // Draw the foreground of the widget
        val symbol = when (currentMode) {
            AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.outline_mobile_vibrate_48
            else -> R.drawable.outline_volume_off_48
        }
        drawable = AppCompatResources.getDrawable(context, symbol) as Drawable
        views.setImageViewBitmap(
            R.id.logo,
            drawBitmap(drawable, storage.foregroundColor, storage.widgetScale)
        )

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
        fun getRingerMode(manager: AudioManager): Int {
            val ringer = manager.ringerMode
            val mute = manager.getStreamVolume(AudioManager.STREAM_RING)

            return if (ringer == AudioManager.RINGER_MODE_NORMAL) {
                if (mute > 0) {
                    AudioManager.RINGER_MODE_NORMAL
                } else {
                    AudioManager.RINGER_MODE_SILENT
                }
            } else if (ringer == AudioManager.RINGER_MODE_VIBRATE) {
                AudioManager.RINGER_MODE_VIBRATE
            } else { // AudioManager.RINGER_MODE_SILENT) {
                AudioManager.RINGER_MODE_SILENT
            }
        }

        @JvmStatic
        fun setRingerMode(manager: AudioManager, mode: Int): Int {
            val pseudoMode: Int

            when (mode) {
                AudioManager.RINGER_MODE_NORMAL -> {
                    pseudoMode = mode
                    manager.adjustStreamVolume(
                        AudioManager.STREAM_RING,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    manager.adjustStreamVolume(
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                    manager.adjustStreamVolume(
                        AudioManager.STREAM_SYSTEM,
                        AudioManager.ADJUST_UNMUTE,
                        0
                    )
                }

                AudioManager.RINGER_MODE_VIBRATE -> {
                    pseudoMode = mode
                }

                else -> { // AudioManager.RINGER_MODE_SILENT) {
                    pseudoMode = AudioManager.RINGER_MODE_NORMAL
                    manager.adjustStreamVolume(
                        AudioManager.STREAM_RING,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                    manager.adjustStreamVolume(
                        AudioManager.STREAM_NOTIFICATION,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                    manager.adjustStreamVolume(
                        AudioManager.STREAM_SYSTEM,
                        AudioManager.ADJUST_MUTE,
                        0
                    )
                }
            }
            return pseudoMode
        }

        @JvmStatic
        fun updateWidget(context: Context) {
            val updateIntent = Intent()
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            context.sendBroadcast(updateIntent)
        }

        // Set the color on a widget component
        @JvmStatic
        fun drawBitmap(drawable: Drawable, color: Int, scale: Float): Bitmap {

            val intrinsicWidth = drawable.intrinsicWidth
            val intrinsicHeight = intrinsicWidth

            // Create a bitmap and canvas the same size as the drawable
            val bmp = createBitmap(intrinsicWidth, (intrinsicHeight * 1.5f).toInt())
            val canvas = Canvas(bmp)

            // Create secondary bitmap and canvas
            val bmp2 = createBitmap(intrinsicWidth, (intrinsicHeight * 1.5f).toInt())
            val canvas2 = Canvas(bmp2)

            // Fill with primary bitmap with the desired color.  This fills the entire canvas
            val paint = Paint()
            paint.color = color
            paint.alpha = 0xff
            paint.style = Paint.Style.FILL
            canvas.drawPaint(paint)

            // Draw the image on the secondary canvas
            drawable.setBounds(0, 0, canvas.width, canvas.width)
            canvas2.scale(scale, scale, canvas.width / 2f, canvas.width / 2f)

            drawable.draw(canvas2)

            // Merge the image and the color
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            canvas.drawBitmap(bmp2, 0f, 0f, paint)

            return bmp
        }

        @JvmStatic
        fun drawTextBitmap(message: String, scale: Float): Bitmap {
            val intrinsicHeight = 160
            val intrinsicWidth = intrinsicHeight
            // Create a bitmap and canvas the same size as the drawable
            val bmp = createBitmap(intrinsicWidth, (intrinsicHeight * 1.5f).toInt())
            val canvas = Canvas(bmp)

            val textPaint = Paint()
            textPaint.setColor(Color.White.toArgb())
            textPaint.textSize = 40f // Set text size in pixels
            textPaint.isAntiAlias = true // Smooth the text edges
            textPaint.textAlign = Paint.Align.CENTER

            canvas.scale(scale, scale, canvas.width / 2f, canvas.width / 2f)

            canvas.drawText(message, canvas.width / 2f, canvas.height * 0.9f, textPaint)

            return bmp
        }
    }

}