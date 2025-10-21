package org.khpylon.ringcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import org.khpylon.ringcontrol.GlanceWidget.Companion.drawIcon
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

class MainActivity : ComponentActivity() {

    @SuppressLint("NewApi")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext
        val storage = Storage(context)

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

                // Create variables to store display state
                var displayMenu by remember { mutableStateOf(false) }

                var displayInstructions by remember { mutableStateOf(false) }
                var displayWhatsNew by remember { mutableStateOf(storage.newInstall) }
                var displayAppInfo by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(context.getString(R.string.app_name)) },
                            actions = {

                                // Creating Icon button for dropdown menu
                                IconButton(onClick = { displayMenu = !displayMenu }) {
                                    Icon(Icons.Default.MoreVert, "")
                                }

                                // Create a dropdown menu
                                DropdownMenu(
                                    expanded = displayMenu,
                                    onDismissRequest = { displayMenu = false },
                                ) {
                                    // Add in each menu item
                                    DropdownMenuItem(
                                        onClick = {
                                            displayMenu = false
                                            displayInstructions = true
                                        },
                                        text = { Text(text = stringResource(R.string.app_usage_menu)) }
                                    )
                                    DropdownMenuItem(
                                        onClick = {
                                            displayMenu = false
                                            displayWhatsNew = true
                                        },
                                        text = { Text(text = stringResource(R.string.release_notes_menu)) }
                                    )
                                    DropdownMenuItem(
                                        onClick = {
                                            displayMenu = false
                                            displayAppInfo = true
                                        },
                                        text = { Text(text = stringResource(R.string.about_menu)) }
                                    )
                                }

                                // If the app has been updated, display a dialog explaining changes
                                if (displayWhatsNew) {
                                    storage.newInstall = false
                                    val description = readChangeFile(context)

                                    // Create a bullet list from each line of text.  Props to
                                    // https://stackoverflow.com/questions/70724196 for the hint.
                                    val bulletString = "\u2022\t"
                                    val textStyle = LocalTextStyle.current
                                    val textMeasurer = rememberTextMeasurer()
                                    val bulletStringWidth = remember(textStyle, textMeasurer) {
                                        textMeasurer.measure(
                                            text = bulletString,
                                            style = textStyle
                                        ).size.width
                                    }
                                    val restLine =
                                        with(LocalDensity.current) { bulletStringWidth.toSp() }
                                    val paragraphStyle =
                                        ParagraphStyle(textIndent = TextIndent(restLine = restLine))
                                    val message = buildAnnotatedString {
                                        description.forEach { text ->
                                            withStyle(style = paragraphStyle) {
                                                append(bulletString)
                                                append(text)
                                            }
                                        }
                                    }

                                    InfoDialog(
                                        onDismissRequest = { displayWhatsNew = false },
                                        dialogTitle = stringResource(R.string.whats_new) + BuildConfig.VERSION_NAME,
                                        dialogText = message
                                    )
                                }

                                // Display information about the app itself
                                if (displayAppInfo) {
                                    InfoDialog(
                                        onDismissRequest = { displayAppInfo = false },
                                        dialogTitle = stringResource(R.string.about_the_app_title),
                                        dialogText =
                                            buildAnnotatedString {
                                                append(context.getString(R.string.app_version))
                                                append(": ")
                                                append(BuildConfig.VERSION_NAME)
                                                append("\n\n")
                                                append(stringResource(R.string.about_more_info))
                                            }
                                    )
                                }

                                // Display instructions for using the app
                                if (displayInstructions) {
                                    InfoDialog(
                                        onDismissRequest = { displayInstructions = false },
                                        dialogTitle = stringResource(R.string.app_usage_title),
                                        dialogText =
                                            buildAnnotatedString {

                                                // How to use the widget
                                                withStyle(
                                                    style = SpanStyle(
                                                        fontSize = TextUnit(16f, TextUnitType.Sp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(stringResource(R.string.using_the_widget_title))
                                                }
                                                append("\n\n")
                                                append(stringResource(R.string.using_the_widget_instructions_part1))
                                                append("\"")
                                                append(stringResource(R.string.permission_settings))
                                                append("\".) ")
                                                append(stringResource(R.string.using_the_widget_instructions_part2))

                                                // How to use the calendar with the widget
                                                append("\n\n")
                                                withStyle(
                                                    style = SpanStyle(
                                                        fontSize = TextUnit(16f, TextUnitType.Sp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(stringResource(R.string.using_calendar_title))
                                                }
                                                append("\n\n")
                                                append(stringResource(R.string.using_calendar_part1))
                                                append("\n\n")
                                                append(stringResource(R.string.using_calendar_part2))

                                                // How to find out more about the app
                                                append("\n\n")
                                                withStyle(
                                                    style = SpanStyle(
                                                        fontSize = TextUnit(16f, TextUnitType.Sp),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                ) {
                                                    append(stringResource(R.string.more_info_title))
                                                }
                                                append("\n\n")
                                                append(stringResource(R.string.more_info_part1))

                                            }
                                    )
                                }
                            }
                        )
                    }, modifier = Modifier.fillMaxSize()
                )
                { innerPadding ->
                    MainApplication(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                GlanceWidget.updateWidget(applicationContext)
            }
        }
    }
}

@Composable
fun InfoDialog(
    onDismissRequest: () -> Unit,
    dialogTitle: String,
    dialogText: AnnotatedString,
) {
    AlertDialog(
        icon = {
            Icon(Icons.Default.Info, contentDescription = "")
        },
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(
                text = dialogText,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = { },
        dismissButton = { }
    )
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
private fun readChangeFile(context: Context): List<String> {
    val assetManager = context.assets
    val message = mutableListOf<String>()
    try {
        val inStream = assetManager.open("info.txt")
        val reader = BufferedReader(InputStreamReader(inStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            message.add(line!!)
        }
    } catch (_: Exception) {
    }
    return message
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
private fun WidgetPermissions(context: Context) {
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
private fun WidgetText(
    context: Context, showText: Boolean,
    onClick: (Boolean) -> Unit
) {
    // Toggle description of text on widget
    OptionSwitchRow(
        tooltip = stringResource(R.string.mode_desc_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(context.getString(R.string.enable_mode_description))
            }
        },
        isChecked = showText,
        onClick = onClick,
    )
}

@Composable
fun SampleIcon(
    context: Context,
    scale: Float,
    fgColor: Int,
    bgColor: Int,
    isTextDescription: Boolean
) {
    val drawable =
        AppCompatResources.getDrawable(context, R.drawable.outline_volume_off_48) as Drawable

    val textDescription =
        if (isTextDescription) context.getString(R.string.normal_description) else null

    val bitmap = drawIcon(drawable, fgColor, bgColor, textDescription)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight(0.75f)
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.padding(3.dp))
        Box(
            modifier = Modifier
                .wrapContentSize(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                modifier = Modifier
                    .padding((5f * scale).dp)
                    .height((100f * scale).dp)
                    .width((100f * scale).dp),
                contentDescription = "",
            )
        }
    }
}


@SuppressLint("RestrictedApi")
@Composable
private fun WidgetColorAndSize(context: Context, showText: Boolean) {
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
    )
    {
        val controller = rememberColorPickerController()
        controller.enabled = true

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
                        if (selectedIndex == 0) {
                            bgColor = it.color.toArgb()
                        } else {
                            fgColor = it.color.toArgb() and 0xffffff
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

        SampleIcon(context, widgetScale, fgColor, bgColor, showText)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
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
                    selectedOption = buttonName
                    val index = radioOptions.indexOf(selectedOption)
                    selectedIndex = index
                    initialColor =
                        (if (selectedIndex == 0) bgColor else fgColor) and 0xffffff
                    recomposeColorPicker = !recomposeColorPicker
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
            .padding(horizontal = 8.dp),
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
                widgetScale = it
            },
            modifier = Modifier
                .fillMaxHeight(0.1f)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {

        // "Save" button
        Button(
            onClick = {
                // Store the new widget information
                storage.foregroundColor = fgColor
                storage.backgroundColor = bgColor
                storage.widgetScale = widgetScale
                GlanceWidget.updateWidget(context)
                Toast.makeText(
                    context,
                    context.getString(R.string.changes_saved), Toast.LENGTH_SHORT
                ).show()
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
fun MainApplication(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val storage = Storage(context)
    val config = LocalConfiguration.current

    val notificationManager =
        context.getSystemService(android.app.Activity.NOTIFICATION_SERVICE) as NotificationManager

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

    var showSettings by rememberSaveable { mutableStateOf(!notificationManager.isNotificationPolicyAccessGranted) }
    var showText by rememberSaveable { mutableStateOf(storage.textDescription) }

    if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {

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
                    WidgetText(
                        context, showText,
                        onClick = { value ->
                            showText = value
                            storage.textDescription = value
                            GlanceWidget.updateWidget(context)
                        }
                    )
                    WidgetColorAndSize(context, showText)
                }
            }
        }
    } else {
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
                    WidgetText(
                        context, showText,
                        onClick = { value ->
                            showText = value
                            storage.textDescription = value
                            GlanceWidget.updateWidget(context)
                        }
                    )
                }
            }

            Column(
                modifier = modifier
                    .fillMaxWidth()
//                    .padding(horizontal = 10.dp) // add some space on left and right
            ) {
                if (showSettings) {
                    WidgetPermissions(context)
                } else {
                    WidgetColorAndSize(context, showText)
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
        MainApplication()
    }
}