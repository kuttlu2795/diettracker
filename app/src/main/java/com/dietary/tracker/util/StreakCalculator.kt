package com.dietary.tracker.util

import com.dietary.tracker.data.entities.FoodEntry
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Badge(val emoji: String, val title: String, val earned: Boolean, val description: String)

data class StreakInfo(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val totalLoggedDays: Int,
    val totalEntries: Int,
    val badges: List<Badge>
)

object StreakCalculator {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun compute(entries: List<FoodEntry>): StreakInfo {
        val days = entries.map { LocalDate.parse(DateUtils.dateKey(it.timestamp), fmt) }
            .toSortedSet()
        val totalLoggedDays = days.size
        val totalEntries = entries.size

        if (days.isEmpty()) {
            return StreakInfo(0, 0, 0, 0, badgesFor(0, 0, 0, 0))
        }

        // Current streak: consecutive days ending today (or yesterday, so a not-yet-logged
        // today doesn't zero out an otherwise-live streak).
        val today = LocalDate.now()
        var current = 0
        var cursor = if (days.contains(today)) today else today.minusDays(1)
        while (days.contains(cursor)) {
            current++
            cursor = cursor.minusDays(1)
        }

        // Longest streak: scan the sorted unique days for the longest consecutive run.
        var longest = 1
        var run = 1
        val sorted = days.toList()
        for (i in 1 until sorted.size) {
            run = if (sorted[i] == sorted[i - 1].plusDays(1)) run + 1 else 1
            longest = maxOf(longest, run)
        }
        longest = maxOf(longest, current)

        return StreakInfo(current, longest, totalLoggedDays, totalEntries, badgesFor(current, longest, totalLoggedDays, totalEntries))
    }

    private fun badgesFor(current: Int, longest: Int, totalLoggedDays: Int, totalEntries: Int) = listOf(
        Badge("🔥", "3-Day Streak", current >= 3 || longest >= 3, "Log food 3 days in a row"),
        Badge("🔥", "7-Day Streak", current >= 7 || longest >= 7, "Log food 7 days in a row"),
        Badge("🏆", "30-Day Streak", current >= 30 || longest >= 30, "Log food 30 days in a row"),
        Badge("📒", "First Log", totalEntries >= 1, "Log your first food"),
        Badge("💯", "100 Entries", totalEntries >= 100, "Log 100 food entries in total"),
        Badge("📅", "10 Days Logged", totalLoggedDays >= 10, "Log food on 10 different days")
    )
}
