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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
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

            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode

            val icon = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48
                AudioManager.RINGER_MODE_SILENT -> R.drawable.outline_volume_off_48
                else -> R.drawable.outline_mobile_vibrate_48
            }
            val label = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> context.getString(R.string.normal_description)
                AudioManager.RINGER_MODE_SILENT -> context.getString(R.string.silent_description)
                else -> context.getString(R.string.vibrate_description)
            }

            val size = LocalSize.current

            // Create widget background
            val backgroundBitmap = createBitmap(
                size.width.value.toInt(),
                size.height.value.toInt(),
                Bitmap.Config.ARGB_8888
            )
            val backgroundCanvas = Canvas(backgroundBitmap)
            val paint = Paint().apply {
                color = Storage(context).backgroundColor
                style = Paint.Style.FILL
            }
            backgroundCanvas.drawCircle(
                size.width.value / 2f,
                size.height.value / 2f,
                size.width.value / 2f,
                paint
            )

            // Create primary bitmap and canvas
            val foregroundBitmap = createBitmap(size.width.value.toInt(), size.height.value.toInt())
            val foregroundCanvas = Canvas(foregroundBitmap)

            // Fill the primary bitmap with the desired color.  This fills the entire canvas
            paint.color = Storage(context).foregroundColor
            paint.alpha = 0xff
            paint.style = Paint.Style.FILL
            foregroundCanvas.drawPaint(paint)

            // Create a temporary bitmap and canvas
            val tempBitmap = createBitmap(size.width.value.toInt(), size.height.value.toInt())
            val tempCanvas = Canvas(tempBitmap)

            // Draw the image on the temporary canvas
            val drawable = AppCompatResources.getDrawable(context, icon) as Drawable
            drawable.setBounds(0, 0, backgroundCanvas.width, backgroundCanvas.width)
            drawable.draw(tempCanvas)

            // Merge the image and the color
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            foregroundCanvas.drawBitmap(tempBitmap, 0f, 0f, paint)

            val scale = Storage(context).widgetScale
            GlanceTheme {
                Column(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {
                    Spacer(modifier = GlanceModifier.padding(3.dp))
                    Image(
                        provider = ImageProvider(foregroundBitmap),
                        contentDescription = context.getString(R.string.current_ring_state_icon),
                        modifier = GlanceModifier.wrapContentHeight()
                            .background(ImageProvider(backgroundBitmap))
                            .padding(5.dp)
                            .height((size.height.value * scale).dp)
                            .width((size.width.value * scale).dp)
                            .clickable(actionRunCallback<GlanceWidgetCallback>())
                    )

                    Spacer(modifier = GlanceModifier.padding(3.dp))

                    Text(
                        text = label,
                        style = TextStyle(
                            color = ColorProvider(
                                day = androidx.compose.ui.graphics.Color.White,
                                night = androidx.compose.ui.graphics.Color.White
                            ),
                            textAlign = TextAlign.Center,
                            fontSize = (16.sp * scale)
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()
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
