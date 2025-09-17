package org.khpylon.ringcontrol

import android.app.Activity.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import org.khpylon.ringcontrol.ui.theme.RingControlTheme
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import androidx.compose.ui.Alignment
import androidx.compose.runtime.key

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext

        // This only works in the foreground for Android 8 and above
//        context.registerReceiver(object : BroadcastReceiver() {
//            // Register a receiver to detect external changes to the ringer
//            override fun onReceive(context: Context, intent: Intent) {
//                if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
//                    Widget.updateWidget(context)
//                }
//            }
//        }, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

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
                    MainApplication (
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                Widget.updateWidget(applicationContext)
            }
        }
    }
}

//        // Convert a hex string to an Int
//        try {
//            val value = "#00ff00"
//            val x = value.substring(1).toInt(16)
//            val objNum = Integer.valueOf(value.substring(1), 16)
//        } catch (e: NumberFormatException) {
//            System.err.println("NumberFormatException caught: " + e.message);
//        }

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun MainApplication(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val storage = Storage(context)

    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    var permissions by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            permissions = notificationManager.isNotificationPolicyAccessGranted
        }
    var selectedIndex by remember { mutableIntStateOf(0) }

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

        // This seems like a kludge; it forces HexColorPicker and BrightnessSlider to reposition the wheel
        var recomposeColorPicker by remember { mutableStateOf(false) }
        var bgColor by remember { mutableIntStateOf(storage.backgroundColor and 0xffffff) }
        var fgColor by remember { mutableIntStateOf(storage.foregroundColor and 0xffffff) }
        var initialColor by remember { mutableIntStateOf(bgColor) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
        )
        {
            val controller = rememberColorPickerController()

            Column() {
                key(recomposeColorPicker) {
                    HsvColorPicker(
                        modifier = Modifier
                            .size(240.dp)
                            .align(alignment = Alignment.CenterHorizontally)
                            .padding(10.dp),
                        initialColor = Color(initialColor),
                        controller = controller,
                        onColorChanged = {
                            if (selectedIndex == 0) {
                                bgColor = it.color.toArgb() and 0xffffff
                            } else {
                                fgColor = it.color.toArgb() and 0xffffff
                            }
                        }
                    )

                    BrightnessSlider(
                        modifier = Modifier
                            .width(240.dp)
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

                val bgBitmap = Widget.drawBitmap(bgDrawable,bgColor)

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

                val bitmap = Widget.drawBitmap(drawable,fgColor).asImageBitmap()
                Image(
                    bitmap = bitmap,
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
                mutableStateOf( radioOptions[selectedIndex] )
            }

            radioOptions.forEach { buttonName ->
                RadioButton(
                    selected = (buttonName == selectedOption),
                    onClick = {
                        selectedOption = buttonName
                        val index = radioOptions.indexOf(selectedOption)
                        selectedIndex = index
                        if( selectedIndex == 0) {
                            initialColor = bgColor and 0xffffff
                        } else {
                            initialColor = fgColor and 0xffffff
                        }
                        recomposeColorPicker = !recomposeColorPicker
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
        ) {

            val buttonColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            )

            // "Save" button
            Button(
                onClick = {
                    // Store the current color information
                    storage.foregroundColor = fgColor
                    storage.backgroundColor = bgColor
                    Widget.updateWidget(context)
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
                    if(selectedIndex == 0) {
                        initialColor = bgColor
                    } else {
                        initialColor = fgColor

                    }
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

        }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RingControlTheme {
        MainApplication()
    }
}