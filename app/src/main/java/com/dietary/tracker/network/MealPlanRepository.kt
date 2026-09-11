package com.dietary.tracker.network

data class MealSuggestion(
    val title: String,
    val readyInMinutes: Int?,
    val servings: Int?,
    val sourceUrl: String?,
    val imageUrl: String?
)

data class DailyMealPlanResult(
    val vegetarian: List<MealSuggestion>,
    val standard: List<MealSuggestion>
)

sealed class MealPlanOutcome {
    data class Success(val plan: DailyMealPlanResult) : MealPlanOutcome()
    data class Error(val message: String) : MealPlanOutcome()
    object MissingApiKey : MealPlanOutcome()
}

/**
 * Generates a real daily meal plan (breakfast/lunch/dinner suggestions) sized to the user's
 * target calories, via Spoonacular's meal-planning algorithm. Nothing here is a static list -
 * every recipe title, timing and link comes back from the live API call.
 */
class MealPlanRepository {

    suspend fun fetchDailyPlan(targetCalories: Int): MealPlanOutcome {
        if (!ApiKeys.hasSpoonacular) return MealPlanOutcome.MissingApiKey

        return try {
            val vegResponse = RetrofitClient.spoonacularApi.generateDayPlan(
                apiKey = ApiKeys.spoonacularKey,
                targetCalories = targetCalories,
                diet = "vegetarian"
            )
            val standardResponse = RetrofitClient.spoonacularApi.generateDayPlan(
                apiKey = ApiKeys.spoonacularKey,
                targetCalories = targetCalories,
                diet = null
            )
            MealPlanOutcome.Success(
                DailyMealPlanResult(
                    vegetarian = vegResponse.meals.orEmpty().map { it.toSuggestion() },
                    standard = standardResponse.meals.orEmpty().map { it.toSuggestion() }
                )
            )
        } catch (e: Exception) {
            MealPlanOutcome.Error(e.message ?: "Couldn't fetch meal suggestions. Check your connection.")
        }
    }

    private fun SpoonacularMeal.toSuggestion() = MealSuggestion(
        title = title ?: "Suggested meal",
        readyInMinutes = readyInMinutes,
        servings = servings,
        sourceUrl = sourceUrl,
        imageUrl = imageUrl()
    )
}
