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
}

data class SpoonacularIngredientMatch(
    val id: Long? = null,
    val title: String? = null,
    val image: String? = null,
    val usedIngredientCount: Int? = null,
    val missedIngredientCount: Int? = null
)
