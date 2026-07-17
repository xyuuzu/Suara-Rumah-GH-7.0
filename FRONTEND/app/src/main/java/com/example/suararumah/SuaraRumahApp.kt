package com.example.suararumah

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class SuaraRumahApp : Application() {

    companion object {
        const val CHANNEL_ID_MONITORING = "suara_rumah_monitoring"
        const val CHANNEL_ID_ALERT = "suara_rumah_alert"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel untuk foreground service monitoring
            val monitoringChannel = NotificationChannel(
                CHANNEL_ID_MONITORING,
                "Pemantauan Aktif",
                NotificationManager.IMPORTANCE_LOW // Low: tidak ada suara, hanya ikon di status bar
            ).apply {
                description = "Notifikasi saat Suara Rumah sedang memantau suara ambient"
                setShowBadge(false)
            }

            // Channel untuk alert darurat
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "Peringatan Darurat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi saat anomali suara terdeteksi"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(monitoringChannel)
            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}
