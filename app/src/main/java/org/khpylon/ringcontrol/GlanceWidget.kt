package org.khpylon.ringcontrol

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.graphics.Bitmap
import android.media.AudioManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlanceWidget : GlanceAppWidget() {

    companion object {
        internal val ICON_SQUARE = DpSize(60.dp, 60.dp)
        internal val MEDIUM_SQUARE = DpSize(100.dp, 100.dp)
        val updateKey: Preferences.Key<Boolean> = booleanPreferencesKey("updated")

        @JvmStatic
        fun updateWidget(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = GlanceAppWidgetManager(context)
                val glanceIds = manager.getGlanceIds(GlanceWidget::class.java)
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, glanceId) { prefs ->
                        val lastState = prefs[updateKey] ?: false
                        prefs[updateKey] = !lastState
                    }
                    GlanceWidget().update(context, glanceId)
                }
            }
        }

        // Assemble the icon used in the widget and the app
        @JvmStatic
        fun drawIcon(
            drawable: Drawable, fgColor: Int, bgColor: Int, textDescription: String?
        ): Bitmap {
            val size = DpSize(150.dp, 150.dp)

            // Create circular background centered in the image space
            val bgBmp = createBitmap(
                size.width.value.toInt(),
                size.height.value.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val bgCanvas = Canvas(bgBmp)
            val paint = Paint().apply {
                color = bgColor
                style = Paint.Style.FILL
            }
            bgCanvas.drawCircle(
                size.width.value / 2f,
                size.height.value / 2f,
                size.width.value / 2f,
                paint
            )

            // Create a bitmap and canvas the same size as the drawable
            val fgBmp = createBitmap(size.width.value.toInt(), size.height.value.toInt())
            val fgCanvas = Canvas(fgBmp)

            // Create secondary bitmap and canvas
            val auxBgBmp = createBitmap(size.width.value.toInt(), size.height.value.toInt())
            val auxBgCanvas = Canvas(auxBgBmp)

            // Fill with primary bitmap with the desired color.  This fills the entire canvas
            paint.color = fgColor
            paint.alpha = 0xff
            paint.style = Paint.Style.FILL
            fgCanvas.drawPaint(paint)

            // Draw the image on the secondary canvas
            val fgOffset = (fgBmp.width * 0.1f).toInt()
            drawable.setBounds(fgOffset, fgOffset, fgBmp.width - fgOffset, fgBmp.height - fgOffset)
            drawable.draw(auxBgCanvas)

            // Merge the image and the color
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            fgCanvas.drawBitmap(auxBgBmp, 0f, 0f, paint)

            // Draw image over background
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OVER)
            fgCanvas.drawBitmap(bgBmp, 0f, 0f, paint)

            // If descriptions are enabled, add the text
            textDescription?.let {

                // Clear the canvas to start over
                auxBgCanvas.drawColor(Color.Transparent.toArgb())

                // Set text attributes
                paint.setColor(Color.White.toArgb())
                paint.textSize = 32f // Set text size in pixels
                paint.isAntiAlias = true // Smooth the text edges
                paint.textAlign = Paint.Align.CENTER

                // Draw text at bottom of canvas
                auxBgCanvas.drawText(
                    textDescription,
                    auxBgCanvas.width / 2f, auxBgCanvas.height * 1.0f, paint
                )

                // Merge in the text
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
                fgCanvas.drawBitmap(auxBgBmp, 0f, 0f, paint)
            }
            return fgBmp
        }
    }

    override val sizeMode = SizeMode.Responsive(
        setOf(
            ICON_SQUARE,
            MEDIUM_SQUARE,
        )
    )

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) {

//        val orientation = context.resources.configuration.orientation
        provideContent {
            // This variable is used to trigger recomposition, I'm pretty sure.  So it's not "unused"
            val update = currentState(key = updateKey) ?: false

            val storage = Storage(context)
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode

            // Get icon for mode
            val icon = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48
                AudioManager.RINGER_MODE_SILENT -> R.drawable.outline_volume_off_48
                else -> R.drawable.outline_mobile_vibrate_48
            }

            // Get description for mode
            val label = if (storage.textDescription) {
                when (currentMode) {
                    AudioManager.RINGER_MODE_NORMAL -> context.getString(R.string.normal_description)
                    AudioManager.RINGER_MODE_SILENT -> context.getString(R.string.silent_description)
                    else -> context.getString(R.string.vibrate_description)
                }
            } else ""

            // Assemble the icon
            val drawable = AppCompatResources.getDrawable(context, icon) as Drawable
            val bitmap = drawIcon(
                drawable, storage.foregroundColor, storage.backgroundColor,label
            )

            val size = LocalSize.current
            val scale = storage.widgetScale
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .wrapContentSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.Top
                ) {
                    Image(
                        provider = ImageProvider(bitmap),
                        contentDescription = context.getString(R.string.current_ring_state_icon),
                        modifier = GlanceModifier
                            .padding(5.dp)
                            .width((size.width.value * scale).dp)
                            .height((size.height.value * scale).dp)
                            .clickable(actionRunCallback<GlanceWidgetCallback>())
                    )
                }
            }
        }
    }
}

class GlanceWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode
            val nextMode =
                when (currentMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_SILENT
                    AudioManager.RINGER_MODE_SILENT -> AudioManager.RINGER_MODE_VIBRATE
                    else -> AudioManager.RINGER_MODE_NORMAL
                }

            val lastState = prefs[GlanceWidget.updateKey] ?: false
            audioManager.ringerMode = nextMode
            prefs[GlanceWidget.updateKey] = !lastState
        }
        GlanceWidget().update(context, glanceId)
    }
}

class GlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = GlanceWidget()
}
