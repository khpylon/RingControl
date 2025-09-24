package org.khpylon.ringcontrol

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetManager
import android.graphics.drawable.Drawable
import android.icu.text.MessageFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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

class MainActivity : ComponentActivity() {
    @SuppressLint("NewApi")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext

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
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                Widget.updateWidget(applicationContext)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Since number of widgets could change while paused, this is an easy way to make sure 
        // composable doesn't get the wrong info
        finish()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApplication(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val storage = Storage(context)

    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    var permissions by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    var isTextVisible by remember { mutableStateOf(storage.textVisible) }
    var isTextDescriptive by remember { mutableStateOf(storage.textDescription) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var showDialog by remember { mutableStateOf(storage.newInstall) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            permissions = notificationManager.isNotificationPolicyAccessGranted
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

    Column(
        modifier = modifier
            .padding(horizontal = 10.dp) // add some space on left and right
    ) {

        // Description of the app
        Text(
            text = stringResource(R.string.app_explanation),
            modifier = Modifier
                .padding(10.dp)
        )

        // Toggle control for DND permissions
        Row(
            modifier = Modifier
                .fillMaxWidth()
        )
        {
            Text(
                text = stringResource(R.string.dnd_permissions),
                modifier = Modifier
                    .padding(10.dp)
            )
            Spacer(Modifier.weight(1f))  // separate text and toggle switch
            Switch(
                checked = permissions,
                onCheckedChange = {
                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    launcher.launch(intent)
                }
            )
        }

        // Check whether there are any widgets on the screen
        val manager = AppWidgetManager.getInstance(context)
        val myWidgetProvider = ComponentName(context, Widget::class.java)
        val enabled = manager.getAppWidgetIds(myWidgetProvider).size > 0

        if (!enabled) {
            Row(modifier = Modifier.fillMaxWidth())
            {
                Text(
                    text = stringResource(R.string.no_widgets_notice),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .alpha(if (enabled) 1.0f else 0.5f)
        ) {
            // Toggle visibility of text on widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            )
            {
                Text(
                    text = "Visible description",
                    modifier = Modifier
                        .padding(10.dp)
                )
                Spacer(Modifier.weight(1f))  // separate text and toggle switch
                if (enabled) {
                    Switch(
                        checked = isTextVisible,
                        onCheckedChange = {
                            isTextVisible = it
                            storage.textVisible = isTextVisible
                            Widget.updateWidget(context)
                        }
                    )
                } else {
                    Switch(
                        checked = isTextVisible,
                        onCheckedChange = null
                    )

                }
            }

            // Toggle description of text on widget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            )
            {
                Text(
                    text = "Enable mode description",
                    modifier = Modifier
                        .padding(10.dp)
                )
                Spacer(Modifier.weight(1f))  // separate text and toggle switch
                if (enabled) {
                    Switch(
                        checked = isTextDescriptive,
                        onCheckedChange = {
                            isTextDescriptive = it
                            storage.textDescription = isTextDescriptive
                            Widget.updateWidget(context)
                        }
                    )
                } else {
                    Switch(
                        checked = isTextDescriptive,
                        onCheckedChange = null
                    )
                }
            }

            // This seems like a kludge; it forces HexColorPicker and BrightnessSlider to reposition the wheel
            var recomposeColorPicker by remember { mutableStateOf(false) }
            var bgColor by remember { mutableIntStateOf(storage.backgroundColor) }
            var fgColor by remember { mutableIntStateOf(storage.foregroundColor and 0xffffff) }
            var initialColor by remember { mutableIntStateOf(bgColor) }
            var widgetScale by remember { mutableFloatStateOf(storage.widgetScale) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            )
            {
                val controller = rememberColorPickerController()
                controller.enabled = enabled

                Column {
                    key(recomposeColorPicker) {
                        HsvColorPicker(
                            modifier = Modifier
                                .size(180.dp)
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
                                .width(180.dp)
                                .align(alignment = Alignment.CenterHorizontally)
                                .padding(10.dp)
                                .height(30.dp),
                            initialColor = Color(initialColor),
                            controller = controller,
                        )
                    }
                }

                Box {
                    val bgDrawable = AppCompatResources.getDrawable(
                        context,
                        R.drawable.background
                    ) as Drawable

                    val bgBitmap = Widget.drawBitmap(bgDrawable, bgColor, widgetScale)
                    Image(
                        bitmap = bgBitmap.asImageBitmap(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
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
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(10.dp),
                        contentDescription = "",
                    )

                    val textBitmap = Widget.drawTextBitmap("Label", widgetScale).asImageBitmap()
                    Image(
                        bitmap = textBitmap,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .padding(20.dp),
                        contentDescription = "",
                    )

                }
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
                            .size(30.dp)
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
                horizontalArrangement = Arrangement.Center
            )
            {
                Text(
                    text = "Widget size",
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
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                val buttonColors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )

                // "Save" button
                Button(
                    onClick = {
                        if (enabled) {
                            // Store the current color information
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
                    Text(text = "Save")
                }

                // "Reset" button
                Button(
                    onClick = {
                        // Reload the initial values
                        fgColor = storage.foregroundColor
                        bgColor = storage.backgroundColor
                        widgetScale = storage.widgetScale
                        initialColor = if (selectedIndex == 0) bgColor else fgColor
                        recomposeColorPicker = !recomposeColorPicker
                    },
                    colors = buttonColors,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                ) {
                    Text(text = "Reset")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.Center
            )
            {}
            Spacer(Modifier.weight(1f))
            Text(
                text = "App version " + BuildConfig.VERSION_NAME,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RingControlTheme {
        MainApplication()
    }
}