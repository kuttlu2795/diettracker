package com.dietary.tracker.network

import com.google.gson.annotations.SerializedName

data class SpoonacularMealPlanResponse(
    @SerializedName("meals") val meals: List<SpoonacularMeal>? = null,
    @SerializedName("nutrients") val nutrients: SpoonacularPlanNutrients? = null
)

data class SpoonacularMeal(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("readyInMinutes") val readyInMinutes: Int? = null,
    @SerializedName("servings") val servings: Int? = null,
    @SerializedName("sourceUrl") val sourceUrl: String? = null,
    @SerializedName("imageType") val imageType: String? = null
) {
    /** Spoonacular doesn't return an image URL directly for meal-plan items, only an id + imageType. */
    fun imageUrl(): String? =
        if (id != null && imageType != null) "https://spoonacular.com/recipeImages/$id-312x231.$imageType" else null
}

data class SpoonacularPlanNutrients(
    @SerializedName("calories") val calories: Double? = null,
    @SerializedName("protein") val protein: Double? = null,
    @SerializedName("fat") val fat: Double? = null,
    @SerializedName("carbohydrates") val carbohydrates: Double? = null
)
