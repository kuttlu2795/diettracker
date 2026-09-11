package com.dietary.tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WaterReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slotIndex = intent.getIntExtra("slotIndex", 0)
        val cupSizeMl = intent.getIntExtra("cupSizeMl", 250)
        val minutesOfDay = intent.getIntExtra("minutesOfDay", 0)

        NotificationHelper.showWaterReminder(context, cupSizeMl, slotIndex)
        WaterAlarmScheduler.rescheduleNextDay(context, slotIndex, minutesOfDay, cupSizeMl)
    }
}
