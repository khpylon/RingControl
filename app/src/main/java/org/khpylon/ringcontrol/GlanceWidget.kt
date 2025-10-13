package org.khpylon.ringcontrol

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.util.Log
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
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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
                    updateAppWidgetState(context,glanceId) { prefs ->
                        val lastState = prefs[GlanceWidget.updateKey] ?: false
                        prefs[GlanceWidget.updateKey] = !lastState
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
            val update = currentState( key = updateKey) ?: false

            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            val currentMode = audioManager.ringerMode

            val icon = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> R.drawable.outline_volume_up_48
                AudioManager.RINGER_MODE_SILENT -> R.drawable.outline_volume_off_48
                else -> R.drawable.outline_mobile_vibrate_48
            }
            val label = when (currentMode) {
                AudioManager.RINGER_MODE_NORMAL -> "Normal"
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                else -> "Vibrate"
            }

            val size = LocalSize.current
            val bmp = createBitmap(
                size.width.value.toInt(),
                size.height.value.toInt(),
                Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val paint = Paint().apply {
                color = Storage(context).backgroundColor
                style = Paint.Style.FILL
            }
            canvas.drawCircle(size.width.value/2f, size.height.value/2f,size.width.value/2f, paint)



            val scale = Storage(context).widgetScale
            val mycolor = Storage(context).foregroundColor
///
            val drawable = AppCompatResources.getDrawable(context, icon) as Drawable

            // Create a bitmap and canvas the same size as the drawable
            val bmp_a = createBitmap(size.width.value.toInt(), size.height.value.toInt())
            val canvas_a = Canvas(bmp_a)

            // Create secondary bitmap and canvas
            val bmp_b = createBitmap(size.width.value.toInt(), size.height.value.toInt())
            val canvas_b = Canvas(bmp_b)

            // Fill with primary bitmap with the desired color.  This fills the entire canvas
            paint.color = mycolor
            paint.alpha = 0xff
            paint.style = Paint.Style.FILL
            canvas_a.drawPaint(paint)

            // Draw the image on the secondary canvas
            drawable.setBounds(0, 0, canvas.width, canvas.width)
            drawable.draw(canvas_b)

            // Merge the image and the color
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
            canvas_a.drawBitmap(bmp_b, 0f, 0f, paint)

///


            GlanceTheme {
                Column (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally,

                    ) {
                    Spacer( modifier = GlanceModifier.padding(3.dp))
                    Image(
//                        provider = ImageProvider(icon),
                        provider = ImageProvider(bmp_a),
                        contentDescription = "nothing",
                        modifier = GlanceModifier.wrapContentHeight()
                            .background(ImageProvider(bmp))
                            .padding(5.dp)
                            .height((size.height.value*scale).dp)
                            .width((size.width.value*scale).dp)
                            .clickable(actionRunCallback<GlanceWidgetCallback>())
                    )

                    Spacer( modifier = GlanceModifier.padding(3.dp))

//                    if(size.height > 50.dp)

                    Text(
                        text = label,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            textAlign = TextAlign.Center,
                            fontSize = (16.sp* scale)
                        ),
                        modifier = GlanceModifier
                            .fillMaxWidth()


                    )

                }
            }
        }
    }
}

class GlanceWidgetCallback: ActionCallback {
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
                    AudioManager.RINGER_MODE_SILENT ->  AudioManager.RINGER_MODE_VIBRATE
                    else -> AudioManager.RINGER_MODE_NORMAL
                }

            val lastState = prefs[GlanceWidget.updateKey] ?: false
            audioManager.ringerMode = nextMode
            prefs[GlanceWidget.updateKey] = !lastState
            Log.d("what", "text = changed")
        }
        GlanceWidget().update(context,glanceId)

    }

}

class GlanceWidgetReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = GlanceWidget()

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        QuoteWidgetWorker.enqueue(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

}

class QuoteWidgetWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context,  params) {

    companion object {

        // We will use this worker to update all the widgets of this type at once, so our unique name
        // is just the class name. If you want to have a different worker for each individual widget
        // then you need to specify a unique name per widget
        private val uniqueWorkName = QuoteWidgetWorker::class.java.simpleName

        fun enqueue(context: Context, force: Boolean = false) {
            val workManager = WorkManager.getInstance(context)
            val request =
                PeriodicWorkRequestBuilder<QuoteWidgetWorker>(20, TimeUnit.SECONDS).build()

            workManager.enqueueUniquePeriodicWork(
                uniqueWorkName = uniqueWorkName,
                existingPeriodicWorkPolicy = if (force) {
                    ExistingPeriodicWorkPolicy.UPDATE
                } else {
                    ExistingPeriodicWorkPolicy.KEEP
                },
                request = request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        }
    }





    override suspend fun doWork(): Result {
        val appWidgetManager = GlanceAppWidgetManager(context)
        appWidgetManager.getGlanceIds(GlanceWidget::class.java).forEach { glanceId ->
            // Update the widget with the new state
            updateAppWidgetState(context, glanceId) { prefs ->
                val lastState = prefs[GlanceWidget.updateKey] ?: false
                prefs[GlanceWidget.updateKey] = !lastState
            }
            // Let the widget know there is a new state so it updates the UI
            GlanceWidget().update(context, glanceId)
        }
        return Result.success()
    }
}
