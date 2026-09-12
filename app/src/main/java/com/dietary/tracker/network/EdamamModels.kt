package com.dietary.tracker.network

import com.google.gson.annotations.SerializedName

data class EdamamNutritionResponse(
    @SerializedName("calories") val calories: Double? = null,
    @SerializedName("totalWeight") val totalWeight: Double? = null,
    @SerializedName("totalNutrients") val totalNutrients: EdamamNutrients? = null
)

data class EdamamNutrients(
    @SerializedName("ENERC_KCAL") val energy: EdamamNutrientValue? = null,
    @SerializedName("PROCNT") val protein: EdamamNutrientValue? = null,
    @SerializedName("FAT") val fat: EdamamNutrientValue? = null,
    @SerializedName("CHOCDF") val carbs: EdamamNutrientValue? = null,
    @SerializedName("FIBTG") val fiber: EdamamNutrientValue? = null,
    @SerializedName("SUGAR") val sugar: EdamamNutrientValue? = null,
    @SerializedName("NA") val sodium: EdamamNutrientValue? = null,
    @SerializedName("VITC") val vitaminC: EdamamNutrientValue? = null,
    @SerializedName("VITA_RAE") val vitaminA: EdamamNutrientValue? = null,
    @SerializedName("CA") val calcium: EdamamNutrientValue? = null,
    @SerializedName("FE") val iron: EdamamNutrientValue? = null,
    @SerializedName("K") val potassium: EdamamNutrientValue? = null,
    @SerializedName("MG") val magnesium: EdamamNutrientValue? = null,
    @SerializedName("ZN") val zinc: EdamamNutrientValue? = null,
    @SerializedName("VITD") val vitaminD: EdamamNutrientValue? = null,
    @SerializedName("VITB12") val vitaminB12: EdamamNutrientValue? = null,
    @SerializedName("FOLDFE") val folate: EdamamNutrientValue? = null
)

data class EdamamNutrientValue(
    @SerializedName("quantity") val quantity: Double? = null,
    @SerializedName("unit") val unit: String? = null
)

/** Converts an Edamam response (values for the parsed quantity) into a per-100g NutritionResult. */
fun EdamamNutritionResponse.toNutritionResult(displayName: String): NutritionResult? {
    val weight = totalWeight
    if (weight == null || weight <= 0.0) return null
    val n = totalNutrients ?: return null
    val factor = 100.0 / weight

    return NutritionResult(
        name = displayName,
        caloriesPer100g = (calories ?: 0.0) * factor,
        proteinPer100g = (n.protein?.quantity ?: 0.0) * factor,
        carbsPer100g = (n.carbs?.quantity ?: 0.0) * factor,
        sugarPer100g = (n.sugar?.quantity ?: 0.0) * factor,
        fatPer100g = (n.fat?.quantity ?: 0.0) * factor,
        fiberPer100g = (n.fiber?.quantity ?: 0.0) * factor,
        sodiumMgPer100g = (n.sodium?.quantity ?: 0.0) * factor,
        vitaminCMgPer100g = (n.vitaminC?.quantity ?: 0.0) * factor,
        vitaminAMcgPer100g = (n.vitaminA?.quantity ?: 0.0) * factor,
        calciumMgPer100g = (n.calcium?.quantity ?: 0.0) * factor,
        ironMgPer100g = (n.iron?.quantity ?: 0.0) * factor,
        potassiumMgPer100g = (n.potassium?.quantity ?: 0.0) * factor,
        magnesiumMgPer100g = (n.magnesium?.quantity ?: 0.0) * factor,
        zincMgPer100g = (n.zinc?.quantity ?: 0.0) * factor,
        vitaminDMcgPer100g = (n.vitaminD?.quantity ?: 0.0) * factor,
        vitaminB12McgPer100g = (n.vitaminB12?.quantity ?: 0.0) * factor,
        folateMcgPer100g = (n.folate?.quantity ?: 0.0) * factor,
        source = "Edamam (analyzed from: \"$displayName\")"
    )
}
