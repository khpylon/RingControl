package org.khpylon.ringcontrol

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class RingerModeService : Service() {
    private val CHANNEL_ID = "ringer_service_channel"
    private val NOTIFICATION_ID = 101

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val mode = intent?.getIntExtra("MODE", AudioManager.RINGER_MODE_NORMAL) ?: AudioManager.RINGER_MODE_NORMAL
        // 1. Build and display the mandatory persistent notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ringer Controller Active")
            .setContentText("Managing device audio profile in the background.")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // 2. Start Foreground (Must include service type for API 34+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // 3. Safe to run execution logic now that the app is in Foreground context
        changeRingerMode(mode)

        // Stop the service right after completion if it's a one-shot operation
        stopSelf()

        return START_NOT_STICKY
    }
    private fun changeRingerMode(mode: Int) {
        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        try {
            audioManager.ringerMode = mode

        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ringer Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

}