package com.dietary.tracker.util

import kotlin.math.roundToInt

object Calculations {

    /** BMI = weight(kg) / height(m)^2 */
    fun bmi(weightKg: Float, heightCm: Float): Double {
        if (heightCm <= 0f) return 0.0
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi <= 0.0 -> "-"
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Normal"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    /** Mifflin-St Jeor Basal Metabolic Rate, kcal/day */
    fun bmr(weightKg: Float, heightCm: Float, age: Int, gender: String): Double {
        val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age
        return if (gender.equals("Female", ignoreCase = true)) base - 161.0 else base + 5.0
    }

    private val ACTIVITY_MULTIPLIERS = mapOf(
        "Sedentary" to 1.2,
        "Light" to 1.375,
        "Moderate" to 1.55,
        "Active" to 1.725,
        "Very Active" to 1.9
    )

    /** Total Daily Energy Expenditure: BMR * activity factor */
    fun tdee(bmrValue: Double, activityLevel: String): Double {
        val factor = ACTIVITY_MULTIPLIERS[activityLevel] ?: 1.375
        return bmrValue * factor
    }

    /**
     * Extra calories burnt from walking, on top of BMR.
     * Rough approximation: ~0.04 kcal per step for a 70kg adult, scaled linearly by body weight.
     */
    fun caloriesFromSteps(steps: Int, weightKg: Float): Double {
        val perStepAt70kg = 0.04
        val scale = weightKg / 70.0
        return steps * perStepAt70kg * scale
    }

    /**
     * Estimated bodyweight change in kg from a calorie balance.
     * ~7700 kcal surplus/deficit ≈ 1 kg of fat gained/lost.
     * Positive result = weight gain, negative = weight loss.
     */
    fun weightChangeKg(calorieBalance: Double): Double = calorieBalance / 7700.0

    fun round1(value: Double): Double = (value * 10.0).roundToInt() / 10.0
}
