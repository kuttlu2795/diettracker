package com.dietary.tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dietary.tracker.DietTrackerApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = context.applicationContext as DietTrackerApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = app.repository.getWaterReminder()
                if (settings.enabled) {
                    val profile = app.repository.getProfile()
                    WaterAlarmScheduler.scheduleAll(context, settings, profile)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
