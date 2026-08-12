package org.khpylon.ringcontrol

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale


@Composable
fun WidgetPermissions(context: Context) {
    val storage = Storage(context)
    val notificationManager =
        context.getSystemService(Activity.NOTIFICATION_SERVICE) as NotificationManager
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

    // Can the app use calendar events
    var calendarPermission by remember {
        mutableStateOf(storage.isCalendarEnabled)
    }

    // Is the user able to see the calendar permission dialog?
    var calendarPermissionPopup by remember {
        mutableStateOf(false)
    }

    // Is the user able to see the calendar permission dialog?
    var calendarSettingsPopup by remember {
        mutableStateOf(false)
    }

    // Request calendar permissions if necessary,
    val calendarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                calendarPermission = storage.isCalendarEnabled
                CalendarAlarmReceiver.checkForEvents(context)
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity as Activity, Manifest.permission.READ_CALENDAR)) {
                    calendarPermissionPopup  = true
                }
            }

//            // If the permission is granted, immediately check for events.  If it's not, then display
//            // the pop-up dialog: perhaps the user previously denied permission
//            if (calendarPermission) {
//                CalendarAlarmReceiver.checkForEvents(context)
//            } else {
//                calendarSettingsPopup = true
//            }
        }


    // If request for calendar permissions fails, show a dialog
    if (calendarPermissionPopup) {
        InfoDialog(
            onDismissRequest = { calendarPermissionPopup = false },
            dialogTitle = "Calendar Read Permissions",
            dialogText =
                buildAnnotatedString {
                    append("This permission is needed for the app to scan calender events and automatically change the mode setting.  ")
                    append("If you only want to use the on-screen widget to control the ring mode, you can leave this permission disabled.")
                },
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = "Try again",
            onConfirmRequest = {
                calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                calendarPermissionPopup = false
            },
        )
    }

//    // Force request of calendar permissions dialog,
//    val calendarSettingsLauncher =
//        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
//        {
//            if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
//                calendarPermission = storage.isCalendarEnabled
//                CalendarAlarmReceiver.checkForEvents(context)
//            }
//        }

//    InfoDialog(
//        onDismissRequest = { calendarSettingsPopup = false },
//        dialogTitle = stringResource(R.string.calendar_permission_dialog_title),
//        dialogText =
//            buildAnnotatedString {
//                append(stringResource(R.string.calendar_permission_dialog_text))
//            },
//        dismissText = stringResource(R.string.dismiss_button_text),
//        confirmText = stringResource(R.string.setting_button_text),
//        onConfirmRequest = {
//            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
//                // Create a URI pointing specifically to this app's package
//                data = Uri.fromParts("package", packageName, null)
//            }
//            calendarSettingsLauncher.launch(intent)
//            calendarSettingsPopup = false
//        },
//    )


    // Can the app use notifications for calendar events
    var notificationPermission by remember {
        mutableStateOf(storage.isNotificationEnabled)
    }

    // Is the user able to see the app notification dialog?
    var notificationSettingsPopup by remember {
        mutableStateOf(false)
    }

    // Request notification permissions if necessary,
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        {
            storage.isNotificationEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationPermission = storage.isNotificationEnabled
            if (!notificationPermission) {
                notificationSettingsPopup = true
            }
        }

    // Force request of notification permissions dialog,
    val notificationSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            storage.isNotificationEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                    || context.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            notificationPermission = storage.isNotificationEnabled
        }

    // If request for notification permissions fails, show a dialog
    if (notificationSettingsPopup) {
        InfoDialog(
            onDismissRequest = { notificationSettingsPopup = false },
            dialogTitle = stringResource(R.string.notification_permission_dialog_title),
            dialogText =
                buildAnnotatedString {
                    append(stringResource(R.string.notification_permission_dialog_text))
                },
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = stringResource(R.string.setting_button_text),
            onConfirmRequest = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                notificationSettingsLauncher.launch(intent)
                notificationSettingsPopup = false
            },
        )
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
            append(":\n  ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(context.getString(if (modesAccessPermission) R.string.enabled_description else R.string.disabled_description))
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
            append(":\n  ")
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
            append(":\n  ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
                append("Permission denied") } else

                append(context.getString(if (calendarPermission) R.string.enabled_description else R.string.disabled_description))
            }
        },
        isChecked = calendarPermission,
        onClick = { value ->
            if (value) {
                if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    storage.isCalendarEnabled = true
                    calendarPermission = true
                    CalendarAlarmReceiver.checkForEvents(context)
                } else {
                    calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            } else {
                storage.isCalendarEnabled = false
                calendarPermission = false
                CalendarAlarmReceiver.cancelAlarm(context)
            }
        },
        onLongClick = {
            Toast.makeText(context,"long click", Toast.LENGTH_SHORT).show()
        }
    )

    // Enable/disable calendar event notifications
    if (calendarPermission) {
        OptionSwitchRow(
            tooltip = stringResource(R.string.notification_tooltip),
            desc = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(stringResource(R.string.show_notifications_on_calendar_event))
                }
                append(":\n  ")
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(context.getString(if (notificationPermission) R.string.enabled_description else R.string.disabled_description))
                }
            },
            isChecked = notificationPermission,
            onClick = { value ->
                if (value) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            storage.isNotificationEnabled = true
                            notificationPermission = true
                        } else {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        storage.isNotificationEnabled = true
                        notificationPermission = true
                    }
                } else {
                    storage.isNotificationEnabled = false
                    notificationPermission = false
                }
            }
        )
    }

}

