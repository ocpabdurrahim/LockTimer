package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class TabLockApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                "tab_lock_timer_channel",
                "TabLock Timer Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live remaining countdown before tablet screen locks"
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                "tab_lock_alerts_channel",
                "TabLock Warnings & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when tablet is about to lock"
                enableVibration(true)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(timerChannel)
            notificationManager?.createNotificationChannel(alertsChannel)
        }
    }
}
