package com.dietary.tracker.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dietary.tracker.data.entities.UserProfile
import com.dietary.tracker.data.entities.WaterReminderSettings
import com.dietary.tracker.util.NutritionPlanner
import java.util.Calendar

object WaterAlarmScheduler {

    private const val MAX_SLOTS = 20
    private const val REQUEST_CODE_BASE = 9000

    /** Returns the list of times-of-day (minutes since midnight) reminders should fire. */
    fun computeSlotMinutes(settings: WaterReminderSettings, profile: UserProfile): List<Int> {
        val plan = NutritionPlanner.buildPlan(profile)
        val cupSize = settings.cupSizeMl.coerceAtLeast(100)
        val slotCount = (plan.waterMl / cupSize).coerceIn(4, MAX_SLOTS)

        val wakeMinutes = settings.wakeHour * 60 + settings.wakeMinute
        var sleepMinutes = settings.sleepHour * 60 + settings.sleepMinute
        if (sleepMinutes <= wakeMinutes) sleepMinutes += 24 * 60 // handle wrap past midnight

        val span = sleepMinutes - wakeMinutes
        val step = span / (slotCount + 1)
        return (1..slotCount).map { (wakeMinutes + step * it) % (24 * 60) }
    }

    fun scheduleAll(context: Context, settings: WaterReminderSettings, profile: UserProfile) {
        cancelAll(context)
        if (!settings.enabled) return

        val slots = computeSlotMinutes(settings, profile)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        slots.forEachIndexed { index, minutesOfDay ->
            val triggerTime = nextTriggerTimeFor(minutesOfDay)
            val pendingIntent = buildPendingIntent(context, index, settings.cupSizeMl, minutesOfDay)
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (e: SecurityException) {
                // Exact alarm permission not granted on this device/OS version; skip gracefully.
            }
        }
    }

    /** Called by the receiver after a reminder fires, to schedule the same slot for the next day. */
    fun rescheduleNextDay(context: Context, slotIndex: Int, minutesOfDay: Int, cupSizeMl: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
        }
        val pendingIntent = buildPendingIntent(context, slotIndex, cupSizeMl, minutesOfDay)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } catch (e: SecurityException) {
            // ignore
        }
    }

    fun cancelAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (i in 0 until MAX_SLOTS) {
            val pendingIntent = buildPendingIntent(context, i, 0, 0)
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun buildPendingIntent(context: Context, slotIndex: Int, cupSizeMl: Int, minutesOfDay: Int): PendingIntent {
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            putExtra("slotIndex", slotIndex)
            putExtra("cupSizeMl", cupSizeMl)
            putExtra("minutesOfDay", minutesOfDay)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        return PendingIntent.getBroadcast(context, REQUEST_CODE_BASE + slotIndex, intent, flags)
    }

    private fun nextTriggerTimeFor(minutesOfDay: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minutesOfDay / 60)
            set(Calendar.MINUTE, minutesOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
}
