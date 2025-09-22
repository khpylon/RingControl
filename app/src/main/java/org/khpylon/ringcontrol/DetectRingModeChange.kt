package org.khpylon.ringcontrol

//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
import android.app.Service
//import android.content.BroadcastReceiver
//import android.content.Context
import android.content.Intent
//import android.content.IntentFilter
//import android.media.AudioManager
import android.os.IBinder
//import android.os.PowerManager

class DetectRingModeChange : Service() {
//    private var wakeLock: PowerManager.WakeLock? = null
//    private var isServiceStarted = false

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        intent?.let {
//            if (isServiceStarted) {
//                stopService()
//            }
//            isServiceStarted = true
//
//            // we need this lock so our service gets not affected by Doze Mode
//            wakeLock =
//                (getSystemService(POWER_SERVICE) as PowerManager).run {
//                    newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RingChanges::lock").apply {
//                        acquire()
//                    }
//                }
//
//            this.registerReceiver(object : BroadcastReceiver() {
//                // Register a receiver to detect external changes to the ringer
//                override fun onReceive(context: Context, intent: Intent) {
//                    if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
//                        Widget.updateWidget(context)
//                    }
//                }
//            }, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
//        }
//
//        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

//    override fun onCreate() {
//        super.onCreate()
//        val notification = createNotification()
//        startForeground(1, notification)
//    }

//    private fun stopService () {
//        try {
//            wakeLock?.let {
//                if (it.isHeld) {
//                    it.release()
//                }
//            }
//            stopForeground(true)
//            stopSelf()
//        } catch (_: Exception) {
//        }
//    }
//    private fun createNotification(): Notification {
//        val notificationChannelId = "RING CONTROL SERVICE CHANNEL"
//
//        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        val channel = NotificationChannel(
//            notificationChannelId,
//            getString(R.string.ring_control_notifications_channel),
//            NotificationManager.IMPORTANCE_HIGH
//        ).let {
//            it.description = getString(R.string.ring_control_channel)
//            it.enableVibration(false)
//            it
//        }
//        notificationManager.createNotificationChannel(channel)
//
//        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
//            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
//        }
//
//        return Notification.Builder(this,notificationChannelId)
//            .setSmallIcon(R.drawable.ic_notifier)
//            .setContentTitle(getString(R.string.ring_control_notification_title))
//            .setContentText(getString(R.string.ring_control_notification_text))
//            .setContentIntent(pendingIntent)
//            .build()
//    }
}