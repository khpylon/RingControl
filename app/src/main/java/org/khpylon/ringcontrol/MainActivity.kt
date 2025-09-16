package org.khpylon.ringcontrol

import android.app.Activity.NOTIFICATION_SERVICE
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.khpylon.ringcontrol.ui.theme.RingControlTheme


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val context = applicationContext
        context.registerReceiver(object : BroadcastReceiver() {

            // Register a receiver to detect external changes to the ringer
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
                    Widget.updateWidget(context)
                }
            }
        }, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))

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

@Composable
fun MainApplication(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    var permissions by remember { mutableStateOf(notificationManager.isNotificationPolicyAccessGranted) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            permissions = notificationManager.isNotificationPolicyAccessGranted
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

    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    RingControlTheme {
        MainApplication()
    }
}