package com.dietary.tracker.network

import retrofit2.http.GET
import retrofit2.http.Query

interface SpoonacularApi {
    @GET("mealplanner/generate")
    suspend fun generateDayPlan(
        @Query("apiKey") apiKey: String,
        @Query("timeFrame") timeFrame: String = "day",
        @Query("targetCalories") targetCalories: Int,
        @Query("diet") diet: String? = null
    ): SpoonacularMealPlanResponse

    @GET("recipes/findByIngredients")
    suspend fun findByIngredients(
        @Query("apiKey") apiKey: String,
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 5,
        @Query("ranking") ranking: Int = 1
    ): List<SpoonacularIngredientMatch>

    @GET("food/menuItems/search")
    suspend fun searchMenuItems(
        @Query("apiKey") apiKey: String,
        @Query("query") query: String,
        @Query("number") number: Int = 5
    ): SpoonacularMenuItemSearchResponse

    @GET("food/menuItems/{id}")
    suspend fun getMenuItem(
        @Query("apiKey") apiKey: String,
        @retrofit2.http.Path("id") id: Long
    ): SpoonacularMenuItem
}

data class SpoonacularMenuItemSearchResponse(
    val menuItems: List<SpoonacularMenuItem>? = null
)

data class SpoonacularMenuItem(
    val id: Long? = null,
    val title: String? = null,
    val restaurantChain: String? = null,
    val nutrition: SpoonacularMenuItemNutrition? = null
)

data class SpoonacularMenuItemNutrition(
    val calories: Double? = null,
    val protein: Double? = null,
    val fat: Double? = null,
    val carbs: Double? = null
)

data class SpoonacularIngredientMatch(
    val id: Long? = null,
    val title: String? = null,
    val image: String? = null,
    val usedIngredientCount: Int? = null,
    val missedIngredientCount: Int? = null
)
