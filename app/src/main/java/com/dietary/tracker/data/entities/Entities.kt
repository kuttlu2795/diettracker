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
    val source: String,           // "manual" / "search" / "barcode" / "photo"
    val imageUri: String? = null
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


@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val sugar: Double,
    val fat: Double,
    val fiber: Double,
    val sodium: Double,
    val servings: Int = 1
)
