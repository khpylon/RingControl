package org.khpylon.ringcontrol

import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.khpylon.ringcontrol.ui.theme.RingControlTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applicationContext?.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "android.media.RINGER_MODE_CHANGED") {
                    Widget.updateWidget(context)
                }
            }
        }, IntentFilter("android.media.RINGER_MODE_CHANGED"))

        enableEdgeToEdge()
        setContent {
            RingControlTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    Greeting(
//                        name = "Android",
//                        modifier = Modifier.padding(innerPadding)
//                    )
//                }

                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager

                // the below code is to check the permission that the access notification policy settings from users device..
                if (!notificationManager.isNotificationPolicyAccessGranted
                ) {
                    val intent = Intent(
                        Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                    )
                    startActivity(intent)
                }

//                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
//                val currentMode = audioManager.getRingerMode()
//
//                when (currentMode) {
//                    AudioManager.RINGER_MODE_NORMAL -> audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
//                    AudioManager.RINGER_MODE_VIBRATE -> audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT)
//                    AudioManager.RINGER_MODE_SILENT -> audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
//                }

                Widget.updateWidget(applicationContext)

//                val receiver: BroadcastReceiver = object : BroadcastReceiver() {
//                    override fun onReceive(context: Context, intent: Intent) {
//                        val newIntent = Intent(context, Widget::class.java)
//                        newIntent.action = "REFRESH"
//                        context.sendBroadcast(newIntent)
//                    }
//                }
//                val filter = IntentFilter(
//                    AudioManager.RINGER_MODE_CHANGED_ACTION
//                )
//                registerReceiver(receiver, filter)

                finish()
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RingControlTheme {
        Greeting("Android")
    }
}