package com.dietary.tracker.network

/**
 * Normalized nutrition info per 100g, used regardless of whether the data came
 * from Open Food Facts, Edamam, an AI estimate, manual entry, or OCR.
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
    val badge: String = "db",
    // Micronutrients per 100g - only populated when the source provides them (currently Edamam).
    val vitaminCMgPer100g: Double = 0.0,
    val vitaminAMcgPer100g: Double = 0.0,
    val calciumMgPer100g: Double = 0.0,
    val ironMgPer100g: Double = 0.0,
    val potassiumMgPer100g: Double = 0.0,
    val magnesiumMgPer100g: Double = 0.0,
    val zincMgPer100g: Double = 0.0,
    val vitaminDMcgPer100g: Double = 0.0,
    val vitaminB12McgPer100g: Double = 0.0,
    val folateMcgPer100g: Double = 0.0,
    /** When a source describes an actual portion (e.g. Edamam parsing "2 eggs"), this is that
     *  portion's total weight in grams - lets callers default the quantity field to the exact
     *  described portion instead of an arbitrary 100g. Null when the result is inherently per-100g
     *  (Open Food Facts, AI per-100g estimates) and no specific portion was described. */
    val describedPortionGrams: Double? = null
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
            sodium = sodiumMgPer100g * f,
            vitaminC = vitaminCMgPer100g * f,
            vitaminA = vitaminAMcgPer100g * f,
            calcium = calciumMgPer100g * f,
            iron = ironMgPer100g * f,
            potassium = potassiumMgPer100g * f,
            magnesium = magnesiumMgPer100g * f,
            zinc = zincMgPer100g * f,
            vitaminD = vitaminDMcgPer100g * f,
            vitaminB12 = vitaminB12McgPer100g * f,
            folate = folateMcgPer100g * f
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
    val sodium: Double,
    val vitaminC: Double = 0.0,
    val vitaminA: Double = 0.0,
    val calcium: Double = 0.0,
    val iron: Double = 0.0,
    val potassium: Double = 0.0,
    val magnesium: Double = 0.0,
    val zinc: Double = 0.0,
    val vitaminD: Double = 0.0,
    val vitaminB12: Double = 0.0,
    val folate: Double = 0.0
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
