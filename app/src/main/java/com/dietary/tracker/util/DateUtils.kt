package com.dietary.tracker.util

import java.text.SimpleDateFormat
import java.util.*

enum class RangeType { TODAY, YESTERDAY, WEEK, MONTH, YEAR }

data class TimeRange(val start: Long, val end: Long, val label: String)

object DateUtils {

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val dayLabelFormat = SimpleDateFormat("EEE", Locale.US)

    fun todayKey(): String = dayFormat.format(Date())

    fun dateKey(timestamp: Long): String = dayFormat.format(Date(timestamp))

    fun dayLabel(timestamp: Long): String = dayLabelFormat.format(Date(timestamp))

    fun rangeFor(type: RangeType): TimeRange {
        val cal = Calendar.getInstance()
        // normalize to start of today
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfToday = cal.timeInMillis

        return when (type) {
            RangeType.TODAY -> {
                val end = startOfToday + DAY_MS - 1
                TimeRange(startOfToday, end, "Today")
            }
            RangeType.YESTERDAY -> {
                val start = startOfToday - DAY_MS
                val end = startOfToday - 1
                TimeRange(start, end, "Yesterday")
            }
            RangeType.WEEK -> {
                val c = cal.clone() as Calendar
                c.firstDayOfWeek = Calendar.MONDAY
                c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = c.timeInMillis
                val end = start + 7 * DAY_MS - 1
                TimeRange(start, end, "This Week")
            }
            RangeType.MONTH -> {
                val c = cal.clone() as Calendar
                c.set(Calendar.DAY_OF_MONTH, 1)
                val start = c.timeInMillis
                val c2 = c.clone() as Calendar
                c2.add(Calendar.MONTH, 1)
                val end = c2.timeInMillis - 1
                TimeRange(start, end, "This Month")
            }
            RangeType.YEAR -> {
                val c = cal.clone() as Calendar
                c.set(Calendar.DAY_OF_YEAR, 1)
                val start = c.timeInMillis
                val c2 = c.clone() as Calendar
                c2.add(Calendar.YEAR, 1)
                val end = c2.timeInMillis - 1
                TimeRange(start, end, "This Year")
            }
        }
    }

    fun formatDisplay(timestamp: Long): String = displayFormat.format(Date(timestamp))

    const val DAY_MS = 24L * 60L * 60L * 1000L
}
