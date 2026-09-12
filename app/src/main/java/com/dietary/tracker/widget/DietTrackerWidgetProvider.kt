package com.dietary.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.dietary.tracker.DietTrackerApp
import com.dietary.tracker.MainActivity
import com.dietary.tracker.R
import com.dietary.tracker.util.DateUtils
import com.dietary.tracker.util.RangeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DietTrackerWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_ADD_WATER = "com.dietary.tracker.widget.ACTION_ADD_WATER"

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DietTrackerWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(context, DietTrackerWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                })
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_ADD_WATER) {
            val app = context.applicationContext as DietTrackerApp
            CoroutineScope(Dispatchers.IO).launch {
                app.repository.addWater(250, System.currentTimeMillis())
                refreshAll(context)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val app = context.applicationContext as DietTrackerApp
        val views = RemoteViews(context.packageName, R.layout.widget_diet_tracker)

        val openAppIntent = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openAppIntent)

        val addWaterIntent = PendingIntent.getBroadcast(
            context, 1, Intent(context, DietTrackerWidgetProvider::class.java).setAction(ACTION_ADD_WATER),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_water_button, addWaterIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Fill in today's numbers asynchronously, then push a second, data-filled update.
        CoroutineScope(Dispatchers.IO).launch {
            val range = DateUtils.rangeFor(RangeType.TODAY)
            val food = app.repository.foodEntriesBetween(range.start, range.end).first()
            val water = app.repository.waterEntriesBetween(range.start, range.end).first()
            val calories = food.sumOf { it.calories }.toInt()
            val waterMl = water.sumOf { it.amountMl }

            val filled = RemoteViews(context.packageName, R.layout.widget_diet_tracker)
            filled.setOnClickPendingIntent(R.id.widget_title, openAppIntent)
            filled.setOnClickPendingIntent(R.id.widget_add_water_button, addWaterIntent)
            filled.setTextViewText(R.id.widget_calories, "$calories kcal")
            filled.setTextViewText(R.id.widget_water, "$waterMl ml")
            appWidgetManager.updateAppWidget(appWidgetId, filled)
        }
    }
}
