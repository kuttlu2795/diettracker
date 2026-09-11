package com.dietary.tracker.util

import com.dietary.tracker.data.entities.UserProfile
import kotlin.math.roundToInt

data class MealSplit(val mealType: String, val percent: Int, val calories: Int)

data class NutritionPlan(
    val adjustedCalorieGoal: Int,
    val waterMl: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fiberG: Int,
    val mealSplits: List<MealSplit>,
    val weeksToGoal: Int
)

object NutritionPlanner {

    /**
     * Builds a full daily nutrition plan from the user's profile: adjusted calorie target
     * (based on how fast they want to lose/gain weight), water in ml, protein/carbs/fat/fiber
     * in grams, and how to split calories across meals.
     */
    fun buildPlan(profile: UserProfile): NutritionPlan {
        val bmr = Calculations.bmr(profile.currentWeightKg, profile.heightCm, profile.age, profile.gender)
        val tdee = Calculations.tdee(bmr, profile.activityLevel)

        // Weekly rate -> daily calorie adjustment. 7700 kcal ~= 1 kg of body fat.
        val dailyAdjustment = (profile.goalRateKgPerWeek * 7700.0) / 7.0
        val adjustedCalories = when (profile.goalType) {
            "Lose" -> (tdee - dailyAdjustment)
            "Gain" -> (tdee + dailyAdjustment)
            else -> tdee
        }.coerceAtLeast(1200.0) // never suggest below a safe floor

        // Water: ~35 ml per kg body weight, +350ml bump for higher activity levels.
        val activityBonus = when (profile.activityLevel) {
            "Active", "Very Active" -> 500
            "Moderate" -> 250
            else -> 0
        }
        val waterMl = (profile.currentWeightKg * 35).roundToInt() + activityBonus

        // Protein: higher end of range when losing (preserve muscle) or gaining (build muscle).
        val proteinPerKg = when (profile.goalType) {
            "Lose" -> 2.0
            "Gain" -> 1.8
            else -> 1.2
        }
        val proteinG = (profile.currentWeightKg * proteinPerKg).roundToInt()
        val proteinCalories = proteinG * 4

        // Fat: ~25% of total calories
        val fatCalories = adjustedCalories * 0.25
        val fatG = (fatCalories / 9).roundToInt()

        // Remaining calories go to carbs
        val carbCalories = (adjustedCalories - proteinCalories - fatCalories).coerceAtLeast(0.0)
        val carbsG = (carbCalories / 4).roundToInt()

        // Fiber: ~14g per 1000 kcal (general dietary guideline)
        val fiberG = ((adjustedCalories / 1000.0) * 14).roundToInt()

        // Meal split: Breakfast 25% / Lunch 35% / Dinner 30% / Snacks 10%
        val splitPercents = listOf("Breakfast" to 25, "Lunch" to 35, "Dinner" to 30, "Snacks" to 10)
        val mealSplits = splitPercents.map { (meal, percent) ->
            MealSplit(meal, percent, ((adjustedCalories * percent) / 100).roundToInt())
        }

        val totalToChangeKg = kotlin.math.abs(profile.goalWeightKg - profile.currentWeightKg)
        val weeksToGoal = if (profile.goalRateKgPerWeek > 0f)
            kotlin.math.ceil(totalToChangeKg / profile.goalRateKgPerWeek).toInt()
        else 0

        return NutritionPlan(
            adjustedCalorieGoal = adjustedCalories.roundToInt(),
            waterMl = waterMl,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            fiberG = fiberG,
            mealSplits = mealSplits,
            weeksToGoal = weeksToGoal
        )
    }
}
