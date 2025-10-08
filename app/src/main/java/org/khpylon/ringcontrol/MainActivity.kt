package org.khpylon.ringcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Drawable
import android.icu.text.MessageFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.khpylon.ringcontrol.ui.theme.RingControlTheme
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// This viewmodel is used to store the state of whether or not any widgets are on the desktop
class WidgetViewModel : ViewModel() {

    // true means there are widgets present
    private val _status = MutableLiveData(false)
    val status: LiveData<Boolean> = _status

    // set the status directly
    fun setStatus(value: Boolean) {
        _status.value = value
    }

    // disable status when last widget deleted
    fun widgetsDisabled() {
        _status.value = false
    }

    // enable status when first widget added
    fun widgetsEnabled() {
        _status.value = true
    }
}

class MainActivity : ComponentActivity() {
    var model = WidgetViewModel()

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        val action = intent.action
        if (action.equals(Constants.WIDGETS_ENABLED, ignoreCase = true)) {
            Log.d(Constants.LOGTAG, "onNewIntent with WIDGETS_ENABLED")
            model.widgetsEnabled()
        } else if (action.equals(Constants.WIDGETS_DISABLED, ignoreCase = true)) {
            Log.d(Constants.LOGTAG, "onNewIntent with WIDGETS_DISABLED")
            model.widgetsDisabled()
        }
    }

    @SuppressLint("NewApi")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext

        // Check whether there are any widgets on the screen
        val manager = AppWidgetManager.getInstance(context)
        val myWidgetProvider = ComponentName(context, Widget::class.java)
        model.setStatus(manager.getAppWidgetIds(myWidgetProvider).size > 0)

        // Older versions require permission to write log files
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
        }

        val crashMessage = checkLogcat(context)
        if (crashMessage != null) {
            Toast.makeText(context, crashMessage, Toast.LENGTH_SHORT).show()
        }

        // Start service to detect external changes to the ring mode
//        Intent(this, DetectRingModeChange::class.java).also {
//            startForegroundService(it)
//        }
        AudioChangeReceiver.nextAlarm(context)

        // Android 13 and later require user to allow posting of notifications
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                    registerForActivityResult(ActivityResultContracts.RequestPermission()) { }.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            RingControlTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(context.getString(R.string.app_name)) }
                        )
                    }, modifier = Modifier.fillMaxSize()
                )
                { innerPadding ->
                    MainApplication(
                        modifier = Modifier.padding(innerPadding),
                        model = model
                    )
                }
                Widget.updateWidget(applicationContext)
            }
        }
    }
}

private fun checkLogcat(context: Context): String? {
    try {
        // Dump the crash buffer and exit
        val process = Runtime.getRuntime().exec("logcat -d -b crash")
        val bufferedReader = BufferedReader(
            InputStreamReader(process.inputStream)
        )
        val log = java.lang.StringBuilder()
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            if (line!!.contains("AndroidRuntime:")) {
                log.append("${line}\n")
            }
        }

        // If we find something, write to logcat.txt file
        if (log.isNotEmpty()) {
            val inStream: InputStream = ByteArrayInputStream(
                log.toString().toByteArray(
                    StandardCharsets.UTF_8
                )
            )
            val outputFilename = writeExternalFile(
                context,
                inStream,
                "ringcontrol_logcat-",
                "text/plain"
            )

            // Clear the crash log.
            Runtime.getRuntime().exec("logcat -c")
            return MessageFormat.format(
                context.getString(R.string.logcat_crashfile_formatstring),
                outputFilename
            )
        }
    } catch (_: IOException) {
    }
    return null
}

private fun writeExternalFile(
    context: Context,
    inStream: InputStream,
    baseFilename: String,
    mimeType: String?
): String {
    val time = LocalDateTime.now(ZoneId.systemDefault())
    val outputFilename =
        baseFilename + time.format(DateTimeFormatter.ofPattern("MM-dd-HH:mm:ss", Locale.US))
    try {
        val outStream: OutputStream?
        val fileCollection: Uri =
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, outputFilename)
        contentValues.put(MediaStore.Downloads.MIME_TYPE, mimeType)
        val resolver = context.contentResolver
        val uri = resolver.insert(fileCollection, contentValues)
            ?: throw IOException("Couldn't create MediaStore Entry")
        outStream = resolver.openOutputStream(uri)
        outStream?.let {
//            copyStreams(inStream, outStream)
            var len: Int
            val buffer = ByteArray(65536)
            while (inStream.read(buffer).also { len = it } != -1) {
                outStream.write(buffer, 0, len)
            }
            outStream.close()
        }
    } catch (_: IOException) {
    }
    return outputFilename
}

//
private fun readChangeFile(context: Context): String {
    val assetManager = context.assets
    val message = StringBuilder()
    try {
        val inStream = assetManager.open("info.txt")
        val reader = BufferedReader(InputStreamReader(inStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            message.append(line + "\n")
        }
    } catch (_: Exception) {
    }
    return message.toString()
}

// Composable to display control switches and their descriptions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSwitchRow(
    tooltip: String,
    desc: AnnotatedString,
    isChecked: Boolean,
    onClick: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    )
    {
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip =
                {
                    PlainTooltip {
                        Text(tooltip)
                    }
                },
            state = rememberTooltipState()
        ) {
            Text(
                text = desc,
                modifier = Modifier
                    .padding(10.dp)
            )
        }
        Spacer(Modifier.weight(1f))  // separate text and toggle switch
        Switch(
            checked = isChecked,
            onCheckedChange = onClick
        )
    }
}

@Composable
private fun WidgetPermissions (context: Context) {
    val storage = Storage(context)
    val notificationManager =
        context.getSystemService(android.app.Activity.NOTIFICATION_SERVICE) as NotificationManager
    var modesAccessPermission by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    val modesLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            modesAccessPermission = notificationManager.isNotificationPolicyAccessGranted
        }

    val packageName = context.packageName
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    var batteryOptimized by remember { mutableStateOf(pm.isIgnoringBatteryOptimizations(packageName)) }

    val batteryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            batteryOptimized = pm.isIgnoringBatteryOptimizations(packageName)
        }

    var calPermission by remember {
        mutableStateOf(storage.isCalendarEnabled)
    }

    val calLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        {
            storage.isCalendarEnabled = context.checkSelfPermission(
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
            calPermission = storage.isCalendarEnabled
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    )
    {
        Text(
            text = stringResource(R.string.tooltip_help),
            fontSize = 12.sp,
            modifier = Modifier.padding(5.dp)
        )
    }

    // Toggle control for DND permissions
    OptionSwitchRow(
        tooltip = stringResource(R.string.dnd_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(context.getString(R.string.dnd_permissions))
            }
            append("\n  ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(context.getString(if (modesAccessPermission) R.string.dnd_enabled else R.string.dnd_disabled))
            }
        },
        isChecked = modesAccessPermission,
        onClick = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            modesLauncher.launch(intent)
        }
    )

    // Toggle control for battery optimization
    OptionSwitchRow(
        tooltip = stringResource(R.string.battery_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(context.getString(R.string.battery_opt))
            }
            append("\n  ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(context.getString(if (batteryOptimized) R.string.battery_opts_off_description else R.string.battery_opts_on_description))
            }
        },
        isChecked = batteryOptimized,
        onClick = {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            batteryLauncher.launch(intent)
        }
    )

    // Enable/disable calendar usage
    OptionSwitchRow(
        tooltip = stringResource(R.string.calendar_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(context.getString(R.string.use_calendar_events))
            }
        },
        isChecked = calPermission,
        onClick = { value ->
            if (value) {
                if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    storage.isCalendarEnabled = true
                    calPermission = true
                } else {
                    calLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            } else {
                storage.isCalendarEnabled = false
                calPermission = false
            }
        }
    )
}

@Composable
private fun WidgetText (context: Context, enabled: Boolean) {
    val storage = Storage(context)
    var isTextVisible by remember { mutableStateOf(storage.textVisible) }
    var isTextDescriptive by remember { mutableStateOf(storage.textDescription) }

    if (!enabled) {
        Row(modifier = Modifier.fillMaxWidth())
        {
            Text(
                text = stringResource(R.string.no_widgets_notice),
                modifier = Modifier.padding(10.dp)
            )
        }
    }

    // Toggle visibility of text on widget
    OptionSwitchRow(
        tooltip = stringResource(R.string.visible_desc_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(context.getString(R.string.visible_description))
            }
        },
        isChecked = isTextVisible,
        onClick = {
            if (enabled) {
                isTextVisible = it
                storage.textVisible = isTextVisible
                Widget.updateWidget(context)
            }
        },
        modifier = Modifier.alpha(if (enabled) 1.0f else 0.5f)

    )

    if (isTextVisible) {
        // Toggle description of text on widget
        OptionSwitchRow(
            tooltip = stringResource(R.string.mode_desc_tooltip),
            desc = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(context.getString(R.string.enable_mode_description))
                }
            },
            isChecked = isTextDescriptive,
            onClick = {
                if (enabled) {
                    isTextDescriptive = it
                    storage.textDescription = isTextDescriptive
                    Widget.updateWidget(context)
                }
            },
            modifier = Modifier.alpha(if (enabled) 1.0f else 0.5f)

        )
    }
}

@Composable
private fun WidgetColorAndSize (context: Context, enabled: Boolean)
{
    val storage = Storage(context)

    // This seems like a kludge; it forces HexColorPicker and BrightnessSlider to reposition the wheel
    var recomposeColorPicker by remember { mutableStateOf(false) }
    var bgColor by remember { mutableIntStateOf(storage.backgroundColor) }
    var fgColor by remember { mutableIntStateOf(storage.foregroundColor and 0xffffff) }
    var initialColor by remember { mutableIntStateOf(bgColor) }
    var widgetScale by remember { mutableFloatStateOf(storage.widgetScale) }

    var selectedIndex by remember { mutableIntStateOf(0) }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.background
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .alpha(if (enabled) 1.0f else 0.5f)
    )
    {
        val controller = rememberColorPickerController()
        controller.enabled = enabled

        Column {
            key(recomposeColorPicker) {
                HsvColorPicker(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth(0.5f)
                        .align(alignment = Alignment.CenterHorizontally)
                        .padding(10.dp),
                    initialColor = Color(initialColor),
                    controller = controller,
                    onColorChanged = {
                        if (enabled) {
                            if (selectedIndex == 0) {
                                bgColor = it.color.toArgb()
                            } else {
                                fgColor = it.color.toArgb() and 0xffffff
                            }
                        }
                    }
                )

                BrightnessSlider(
                    modifier = Modifier
                        .fillMaxHeight(0.75f)
                        .fillMaxWidth(0.5f)
                        .align(alignment = Alignment.CenterHorizontally)
                        .padding(10.dp),
                    initialColor = Color(initialColor),
                    controller = controller,
                )
            }
        }

        Box()
        {
            val bgDrawable = AppCompatResources.getDrawable(
                context,
                R.drawable.background
            ) as Drawable

            val bgBitmap = Widget.drawBitmap(bgDrawable, bgColor, widgetScale)
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentDescription = "",
            )
            val drawable = AppCompatResources.getDrawable(
                context,
                R.drawable.outline_volume_off_48
            ) as Drawable

            val bitmap =
                Widget.drawBitmap(drawable, fgColor, widgetScale * .8f).asImageBitmap()
            Image(
                bitmap = bitmap,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentDescription = "",
            )

            val textBitmap = Widget.drawTextBitmap(
                stringResource(R.string.sample_mode_label),
                widgetScale
            ).asImageBitmap()
            Image(
                bitmap = textBitmap,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentDescription = "",
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .alpha(if (enabled) 1.0f else 0.5f),
        horizontalArrangement = Arrangement.Center
    )
    {
        // Set up everything for the radio buttons
        val foregroundString = stringResource(R.string.background_string)
        val backgroundString = stringResource(R.string.foreground_string)
        val radioOptions = listOf(foregroundString, backgroundString)

        var selectedOption by remember {
            mutableStateOf(radioOptions[selectedIndex])
        }

        radioOptions.forEach { buttonName ->
            RadioButton(
                selected = (buttonName == selectedOption),
                onClick = {
                    if (enabled) {
                        selectedOption = buttonName
                        val index = radioOptions.indexOf(selectedOption)
                        selectedIndex = index
                        initialColor =
                            (if (selectedIndex == 0) bgColor else fgColor) and 0xffffff
                        recomposeColorPicker = !recomposeColorPicker
                    }
                },
                modifier = Modifier
                    .fillMaxHeight(0.2f)
                    .padding(start = 8.dp)
            )
            Text(
                text = buttonName,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .alpha(if (enabled) 1.0f else 0.5f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    )
    {
        Text(
            text = stringResource(R.string.widget_size),
            modifier = Modifier
                .padding(10.dp)
        )
        Spacer(Modifier.weight(1f))  // separate text and toggle switch

        Slider(
            value = widgetScale,
            valueRange = 0.75f..1.0f,
            steps = 10,
            onValueChange = {
                if (enabled) {
                    widgetScale = it
                }
            },
            modifier = Modifier
                .fillMaxHeight(0.1f)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .alpha(if (enabled) 1.0f else 0.5f),
        horizontalArrangement = Arrangement.Center
    ) {

        // "Save" button
        Button(
            onClick = {
                if (enabled) {
                    // Store the new widget information
                    storage.foregroundColor = fgColor
                    storage.backgroundColor = bgColor
                    storage.widgetScale = widgetScale
                    Widget.updateWidget(context)
                    Toast.makeText(
                        context,
                        context.getString(R.string.changes_saved), Toast.LENGTH_SHORT
                    ).show()
                }
            },
            colors = buttonColors,
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(text = stringResource(R.string.save_button))
        }

        // "Reset" button
        Button(
            onClick = {
                // Reload the prior values
                fgColor = storage.foregroundColor
                bgColor = storage.backgroundColor
                widgetScale = storage.widgetScale
                initialColor = if (selectedIndex == 0) bgColor else fgColor
                recomposeColorPicker = !recomposeColorPicker
            },
            colors = buttonColors,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp)
        ) {
            Text(text = stringResource(R.string.reset_button))
        }
    }
}

@Composable
private fun DescAndControl(
    initialState: Boolean,
    onClick: (Boolean) -> Unit,
) {

    // Description of the app
    Text(
        text = stringResource(R.string.app_explanation),
        modifier = Modifier
            .padding(10.dp)
    )

    var showSettings = initialState
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.background
    )

    // Toggle display of widget appearance UI or permission control
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    )
    {
        Button(
            onClick = {
                showSettings = !showSettings
                onClick(showSettings)
            },
            colors = buttonColors,
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .padding(horizontal = 5.dp)
        ) {
            Text(
                text = if (!initialState) stringResource(R.string.permission_settings)
                else stringResource(R.string.widget_controls),
            )
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApplication(modifier: Modifier = Modifier, model: WidgetViewModel) {
    val context = LocalContext.current
    val viewModel = viewModel { model }
    val widgetStatus by viewModel.status.observeAsState()
    val enabled = widgetStatus as Boolean

    val storage = Storage(context)
    val config = LocalConfiguration.current

    val notificationManager =
        context.getSystemService(android.app.Activity.NOTIFICATION_SERVICE) as NotificationManager
    var showDialog by remember { mutableStateOf(storage.newInstall) }

    var calPermission by remember {
        mutableStateOf(storage.isCalendarEnabled)
    }

    // If we have permissions to use calendar, handle the alarm for it
    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR)
        == PackageManager.PERMISSION_GRANTED
    ) {
        if (calPermission) {
            CalendarAlarmReceiver.startAlarm(context)
        } else {
            CalendarAlarmReceiver.cancelAlarm(context)
        }
    }

    // If the app has been updated, display a dialog explaining changes
    if (showDialog) {
        BasicAlertDialog(
            onDismissRequest = {
                storage.newInstall = false
                showDialog = false
            },
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.whats_new) + BuildConfig.VERSION_NAME,
                        fontWeight = FontWeight.Bold,
                    )
                    HorizontalDivider(thickness = 2.dp)
                    // Get the description from the asset file
                    val description = readChangeFile(context)
                    Text(text = description)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                showDialog = false
                                storage.newInstall = false
                            }
                        ) {
                            Text(stringResource(R.string.dismiss_button))
                        }
                    }
                }
            }
        }
    }


    var showSettings by rememberSaveable { mutableStateOf(!notificationManager.isNotificationPolicyAccessGranted) }

    if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {

        Column(
            modifier = modifier
                .padding(horizontal = 10.dp) // add some space on left and right
        ) {

            DescAndControl(
                showSettings,
                onClick = {
                    showSettings = !showSettings
                }
            )

            // Display settings information
            if (showSettings) {
                WidgetPermissions(context)
            }

            // Display widget appearance UI
            else {
                Column(
                    modifier = Modifier
                ) {
                    WidgetText (context, enabled)
                    WidgetColorAndSize(context, enabled)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center
                    )
                    {}
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.app_version) + BuildConfig.VERSION_NAME,
                        modifier = Modifier
                            .padding(start = 2.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                }
            }
        }
    }

    else {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp) // add some space on left and right
        ) {

            Column(
                modifier = modifier
                    .fillMaxWidth(0.35f)
//                    .padding(horizontal = 10.dp) // add some space on left and right
            ) {

                DescAndControl(
                    showSettings,
                    onClick = {
                        showSettings = !showSettings
                    })

                // Display settings information
                if (!showSettings) {
                    WidgetText(context, enabled)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.app_version) + BuildConfig.VERSION_NAME,
                    modifier = Modifier
                        .padding(start = 2.dp)
                        .align(Alignment.CenterHorizontally)
                )

            }

            Column(
                modifier = modifier
                    .fillMaxWidth()
//                    .padding(horizontal = 10.dp) // add some space on left and right
            ) {
                if (showSettings) {
                    WidgetPermissions(context)
                } else {
                    WidgetColorAndSize(context, enabled)
                }
            }
        }
    }
}

@SuppressLint("ViewModelConstructorInComposable")
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RingControlTheme {
        MainApplication(model = WidgetViewModel())
    }
}