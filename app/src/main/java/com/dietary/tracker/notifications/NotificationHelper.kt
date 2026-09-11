package com.dietary.tracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "water_reminder_channel"
    const val NOTIFICATION_ID_BASE = 5000

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Water Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Reminds you to drink water throughout the day"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showWaterReminder(context: Context, cupSizeMl: Int, slotIndex: Int) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to hydrate 💧")
            .setContentText("Drink a glass of water (~${cupSizeMl}ml) to stay on track today.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val managerCompat = NotificationManagerCompat.from(context)
        try {
            managerCompat.notify(NOTIFICATION_ID_BASE + slotIndex, notification)
        } catch (e: SecurityException) {
            // POST_NOTIFICATIONS permission not granted; silently skip.
        }
    }
}
