package com.dietary.tracker.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single logged food/drink item. Every time a user adds food, one row is
 * created here. Daily totals (sugar, protein, calories, etc.) are computed by
 * summing all rows for a given day - see FoodDao.getEntriesBetween().
 */
@Entity(tableName = "food_entries")
data class FoodEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timestamp: Long,          // epoch millis when logged
    val mealType: String,         // Breakfast / Lunch / Dinner / Snack
    val quantityLabel: String,    // e.g. "1 serving (100g)"
    val quantityGrams: Double,    // normalized grams used for scaling
    val calories: Double,
    val protein: Double,          // grams
    val carbs: Double,            // grams
    val sugar: Double,            // grams
    val fat: Double,              // grams
    val fiber: Double,            // grams
    val sodium: Double,           // mg
    val source: String,           // "manual" / "search" / "barcode" / "photo" / "voice"
    val imageUri: String? = null,
    // Micronutrients (per the logged quantity, not per 100g) - 0.0 when unknown.
    val vitaminCMg: Double = 0.0,
    val vitaminAMcg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val ironMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val magnesiumMg: Double = 0.0,
    val zincMg: Double = 0.0,
    val vitaminDMcg: Double = 0.0,
    val vitaminB12Mcg: Double = 0.0,
    val folateMcg: Double = 0.0
)

@Entity(tableName = "water_entries")
data class WaterEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val amountMl: Int
)

@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val weightKg: Float
)

@Entity(tableName = "step_entries")
data class StepEntry(
    @PrimaryKey val date: String,   // yyyy-MM-dd, one row per calendar day
    val steps: Int,
    val baseStepsAtBoot: Int        // raw sensor cumulative value captured at day start, for delta calc
)

/**
 * Single-row table (id is always 1) holding the user's profile & daily goals.
 */
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val heightCm: Float = 170f,
    val age: Int = 25,
    val gender: String = "Male",              // Male / Female
    val activityLevel: String = "Moderate",   // Sedentary / Light / Moderate / Active / Very Active
    val currentWeightKg: Float = 70f,
    val goalWeightKg: Float = 65f,
    val goalType: String = "Maintain",        // Lose / Gain / Maintain
    val goalRateKgPerWeek: Float = 0.5f,      // how fast they want to lose/gain, kg/week
    val dailyCalorieGoal: Int = 2000,
    val dailyProteinGoalG: Int = 60,
    val dailyWaterGoalMl: Int = 2500,
    val dailyStepGoal: Int = 8000
)

/**
 * Single-row table (id is always 1) holding the water reminder schedule.
 */
@Entity(tableName = "water_reminder_settings")
data class WaterReminderSettings(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val wakeHour: Int = 7,
    val wakeMinute: Int = 0,
    val sleepHour: Int = 22,
    val sleepMinute: Int = 0,
    val cupSizeMl: Int = 250
)

/**
 * A user-saved "quick add" food/meal (MyFitnessPal-style Favorites). Unlike FoodEntry this has
 * no timestamp - it's a reusable template the user creates from any food editor via "Save as
 * Favorite" and can log again anytime in one tap.
 */
@Entity(tableName = "favorite_foods")
data class FavoriteFood(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val mealType: String,
    val quantityLabel: String,
    val quantityGrams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val sugar: Double,
    val fat: Double,
    val fiber: Double,
    val sodium: Double,
    val source: String
)

/**
 * Exercise/workout log entry. Didn't exist before Chandra - a small real addition (not a
 * voice-only stub) so "add 30 minutes walking" persists genuine data that also shows up in
 * History and the Dashboard's calorie-burn total.
 */
@Entity(tableName = "exercise_entries")
data class ExerciseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,           // e.g. "Walking", "Gym", "Cycling"
    val durationMinutes: Int,
    val caloriesBurned: Double, // computed via MET formula at insert time, not guessed
    val timestamp: Long,
    val source: String = "manual" // "manual" / "voice"
)

/**
 * A fasting session. Also a new small real feature added for Chandra - "start/stop fasting" and
 * "fasting status" operate on genuine rows here, not a fake in-memory flag.
 */
@Entity(tableName = "fasting_sessions")
data class FastingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestamp: Long,
    val endTimestamp: Long? = null, // null while the fast is still active
    val targetHours: Int = 16
)

/**
 * A user-defined alternate phrase ("My pulse", "Pulse sollu") that should trigger the same
 * built-in Chandra action (e.g. HeartRateAction). Default phrases are NOT stored here - they live
 * in ChandraCommandParser's built-in pattern list. This table only holds user-added extras.
 */
@Entity(tableName = "chandra_custom_commands")
data class ChandraCustomCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionId: String,  // matches a ChandraActionId name, e.g. "HEART_RATE"
    val phrase: String
)

/** A short log of recently spoken/typed Chandra commands and what Chandra replied, shown in the UI. */
@Entity(tableName = "chandra_recent_commands")
data class ChandraRecentCommand(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val heardText: String,
    val actionId: String?,   // null if unrecognized
    val response: String
)

/** Persisted Chandra on/off + always-listen settings (single row, id = 1). */
@Entity(tableName = "chandra_settings")
data class ChandraSettings(
    @PrimaryKey val id: Int = 1,
    val assistantEnabled: Boolean = true,
    val alwaysListenEnabled: Boolean = false,
    val preferredLanguage: String = "en" // "en" or "ta" - best-effort, depends on installed TTS voices
)
