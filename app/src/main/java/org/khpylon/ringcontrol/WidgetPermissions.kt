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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    var calendarEnabled by remember {
        mutableStateOf(storage.isCalendarEnabled)
    }

    // Are caledar permissions allowed?
    var calendarPermission by remember {
        mutableIntStateOf(storage.calendarPermission)
    }

    // Is the user able to see the calendar permission dialog?
    var calendarPermissionPopup by remember {
        mutableStateOf(false)
    }

    // Request calendar permissions
    val calendarLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                calendarEnabled = storage.isCalendarEnabled
                storage.calendarPermission = StorageConstants.PERMISSION_GRANTED
                CalendarAlarmReceiver.checkForEvents(context)
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity as Activity,
                        Manifest.permission.READ_CALENDAR
                    )
                ) {
                    calendarPermissionPopup = true
                } else {
                    storage.calendarPermission = StorageConstants.PERMISSION_DENIED
                }
            }
            calendarPermission = storage.calendarPermission
        }

    // Force request of app settings
    val calendarSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                storage.calendarPermission = StorageConstants.PERMISSION_GRANTED
                calendarEnabled = storage.isCalendarEnabled
                CalendarAlarmReceiver.checkForEvents(context)
            } else {
                storage.calendarPermission = StorageConstants.PERMISSION_DENIED
            }
            calendarPermission = storage.calendarPermission
        }

    // Can the app use notifications for calendar events
    var notificationEnabled by remember {
        mutableStateOf(storage.isNotificationEnabled)
    }

    // Are notification permissions allowed?
    var notificationPermission by remember {
        mutableIntStateOf(storage.notificationPermission)
    }

    // Is the user able to see the notification permission dialog?
    var notificationPermissionPopup by remember {
        mutableStateOf(false)
    }

    // Request notification permissions if necessary,
    val notificationLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if (isGranted) {
                notificationEnabled = storage.isNotificationEnabled
                storage.notificationPermission = StorageConstants.PERMISSION_GRANTED
            } else {
                var activity = context
                while (activity is ContextWrapper) {
                    if (activity is Activity) break
                    activity = activity.baseContext
                }
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        activity as Activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                ) {
                    notificationPermissionPopup = true
                } else {
                    storage.notificationPermission = StorageConstants.PERMISSION_DENIED
                }
            }
            notificationPermission = storage.notificationPermission
        }

    // If request for notifications permissions fails, show a dialog
    if (notificationPermissionPopup) {
        InfoDialog(
            onDismissRequest = { calendarPermissionPopup = false },
            dialogTitle = stringResource(R.string.post_notifications_permission_title),
            dialogText =
                buildAnnotatedString {
                    append(stringResource(R.string.post_notifications_permission_text))
                },
            dismissText = stringResource(R.string.dismiss_button_text),
            confirmText = stringResource(R.string.try_again_button_text),
            onConfirmRequest = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                notificationPermissionPopup = false
            },
        )
    }

    // Force request of notification permissions dialog,
    val notificationSettingsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                storage.notificationPermission = StorageConstants.PERMISSION_GRANTED
                notificationEnabled = storage.isNotificationEnabled
            } else {
                storage.notificationPermission = StorageConstants.PERMISSION_DENIED
            }
            notificationPermission = storage.notificationPermission
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
            append("\n  Status: ")
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

    // Toggle for calendar usage
    OptionSwitchRow(
        tooltip = stringResource(R.string.calendar_tooltip),
        desc = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                append(context.getString(R.string.use_calendar_events))
            }
            append("\n  Status: ")
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(
                    when (calendarPermission) {
                        StorageConstants.PERMISSION_NOT_REQUESTED -> stringResource(R.string.not_requested)
                        StorageConstants.PERMISSION_DENIED -> stringResource(R.string.denied)
                        else -> context.getString(if (calendarEnabled) R.string.enabled_description else R.string.disabled_description)
                    }
                )
            }
        },
        isChecked = calendarPermission == StorageConstants.PERMISSION_GRANTED && calendarEnabled,
        onClick = { value ->
            if (value) {
                if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
                    storage.isCalendarEnabled = true
                    calendarEnabled = true
                    CalendarAlarmReceiver.checkForEvents(context)
                } else {
                    calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                }
            } else {
                storage.isCalendarEnabled = false
                calendarEnabled = false
                CalendarAlarmReceiver.cancelAlarm(context)
            }
        },
        onLongClick = {
            if (calendarPermission == StorageConstants.PERMISSION_DENIED) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    // Create a URI pointing specifically to this app's package
                    data = Uri.fromParts("package", packageName, null)
                }
                calendarSettingsLauncher.launch(intent)
            }
        }
    )

    if (calendarEnabled) {
        // Toggle control for battery optimization
        OptionSwitchRow(
            tooltip = stringResource(R.string.battery_tooltip),
            desc = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(context.getString(R.string.battery_opt))
                }
                append("\n  Status: ")
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

        // Toggle for calendar event notifications
        OptionSwitchRow(
            tooltip = stringResource(R.string.notification_tooltip),
            desc = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                    append(stringResource(R.string.show_notifications_on_calendar_event))
                }
                append("\n  Status: ")
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(
                        when (notificationPermission) {
                            StorageConstants.PERMISSION_NOT_REQUESTED -> stringResource(R.string.not_requested)
                            StorageConstants.PERMISSION_DENIED -> stringResource(R.string.denied)
                            else -> context.getString(if (notificationEnabled) R.string.enabled_description else R.string.disabled_description)
                        }
                    )
                }
            },
            isChecked = notificationPermission == StorageConstants.PERMISSION_GRANTED && notificationEnabled,
            onClick = { value ->
                if (value) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                            storage.isNotificationEnabled = true
                            notificationEnabled = true
                        } else {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    } else {
                        storage.isNotificationEnabled = true
                        notificationEnabled = true
                    }
                } else {
                    storage.isNotificationEnabled = false
                    notificationEnabled = false
                }
            },
            onLongClick = {
                if (notificationPermission == StorageConstants.PERMISSION_DENIED) {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    notificationSettingsLauncher.launch(intent)
                }
            }

        )
    }

}

