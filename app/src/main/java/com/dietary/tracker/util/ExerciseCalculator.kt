package com.dietary.tracker.util

/**
 * Calories burned = MET * weight(kg) * duration(hours). MET (Metabolic Equivalent of Task)
 * values below are standard, widely published reference values (Compendium of Physical
 * Activities), not per-exercise calorie totals - the actual number is always computed from the
 * user's real weight and the logged duration.
 */
object ExerciseCalculator {

    private val MET_VALUES = mapOf(
        "walking" to 3.5,
        "walk" to 3.5,
        "running" to 8.0,
        "run" to 8.0,
        "jogging" to 7.0,
        "cycling" to 6.0,
        "biking" to 6.0,
        "gym" to 5.0,
        "weights" to 5.0,
        "weight training" to 5.0,
        "yoga" to 3.0,
        "swimming" to 7.0,
        "hiit" to 8.5,
        "dancing" to 4.5,
        "sports" to 6.5
    )

    private const val DEFAULT_MET = 4.0 // generic moderate-activity fallback

    fun metFor(exerciseName: String): Double {
        val key = exerciseName.trim().lowercase()
        return MET_VALUES.entries.firstOrNull { key.contains(it.key) }?.value ?: DEFAULT_MET
    }

    fun caloriesBurned(exerciseName: String, durationMinutes: Int, weightKg: Float): Double {
        val met = metFor(exerciseName)
        val hours = durationMinutes / 60.0
        return met * weightKg * hours
    }
}
