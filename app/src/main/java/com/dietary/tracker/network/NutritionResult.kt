package com.dietary.tracker.network

/**
 * Normalized nutrition info per 100g, used regardless of whether the data came
 * from the local common-foods table, Open Food Facts search/barcode, or OCR.
 */
data class NutritionResult(
    val name: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val sugarPer100g: Double,
    val fatPer100g: Double,
    val fiberPer100g: Double,
    val sodiumMgPer100g: Double,
    val source: String,
    /** "db" = verified database (Open Food Facts / Edamam), "ai" = AI-estimated, "manual" = user-entered. */
    val badge: String = "db"
) {
    fun scaled(grams: Double): ScaledNutrition {
        val f = grams / 100.0
        return ScaledNutrition(
            grams = grams,
            calories = caloriesPer100g * f,
            protein = proteinPer100g * f,
            carbs = carbsPer100g * f,
            sugar = sugarPer100g * f,
            fat = fatPer100g * f,
            fiber = fiberPer100g * f,
            sodium = sodiumMgPer100g * f
        )
    }
}

data class ScaledNutrition(
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val sugar: Double,
    val fat: Double,
    val fiber: Double,
    val sodium: Double
)

fun Product.toNutritionResult(): NutritionResult? {
    val n = nutriments ?: return null
    if (n.energyKcal100g == null) return null
    return NutritionResult(
        name = productName?.takeIf { it.isNotBlank() } ?: "Unknown product",
        brand = brands,
        imageUrl = imageUrl,
        caloriesPer100g = n.energyKcal100g,
        proteinPer100g = n.proteins100g ?: 0.0,
        carbsPer100g = n.carbs100g ?: 0.0,
        sugarPer100g = n.sugars100g ?: 0.0,
        fatPer100g = n.fat100g ?: 0.0,
        fiberPer100g = n.fiber100g ?: 0.0,
        sodiumMgPer100g = (n.sodium100g ?: 0.0) * 1000.0, // g -> mg
        source = "Open Food Facts"
    )
}
