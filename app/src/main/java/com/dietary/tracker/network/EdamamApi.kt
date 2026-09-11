package com.dietary.tracker.network

import retrofit2.http.GET
import retrofit2.http.Query

interface EdamamApi {
    @GET("api/nutrition-data")
    suspend fun analyzeIngredient(
        @Query("app_id") appId: String,
        @Query("app_key") appKey: String,
        @Query("ingr") ingredient: String,
        @Query("nutrition-type") nutritionType: String = "cooking"
    ): EdamamNutritionResponse
}
