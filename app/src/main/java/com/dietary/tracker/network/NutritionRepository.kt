package com.dietary.tracker.network

/**
 * All nutrition data in this app comes from live online sources - nothing is hardcoded:
 *  - Edamam Nutrition Analysis API: natural-language parsing ("1 medium banana", "100g grilled
 *    chicken breast") - great for fruits, home-cooked meals, generic ingredients.
 *  - Open Food Facts: free, keyless database of packaged/branded products - used for search
 *    and barcode scans.
 *  - Spoonacular restaurant menu items: chain-restaurant dishes (e.g. "Big Mac", "Chipotle bowl").
 * Results from all three are merged so the user sees the best available match.
 */
class NutritionRepository {

    suspend fun search(query: String): List<NutritionResult> {
        val results = mutableListOf<NutritionResult>()

        if (ApiKeys.hasEdamam) {
            try {
                val response = RetrofitClient.edamamApi.analyzeIngredient(
                    appId = ApiKeys.edamamAppId,
                    appKey = ApiKeys.edamamAppKey,
                    ingredient = query
                )
                response.toNutritionResult(query.replaceFirstChar { it.uppercase() })?.let { results.add(it) }
            } catch (e: Exception) {
                // Ignore - fall through to the other sources below.
            }
        }

        try {
            val offResults = RetrofitClient.openFoodFactsApi.searchProducts(query).products
                ?.mapNotNull { it.toNutritionResult() } ?: emptyList()
            results.addAll(offResults)
        } catch (e: Exception) {
            // Ignore - network may be unavailable.
        }

        if (ApiKeys.hasSpoonacular) {
            try {
                val menuResults = RetrofitClient.spoonacularApi.searchMenuItems(
                    apiKey = ApiKeys.spoonacularKey,
                    query = query
                ).menuItems?.mapNotNull { it.toNutritionResult() } ?: emptyList()
                results.addAll(menuResults)
            } catch (e: Exception) {
                // Ignore - restaurant menu search is a bonus source, not required.
            }
        }

        return results
    }

    suspend fun byBarcode(barcode: String): NutritionResult? {
        return try {
            val resp = RetrofitClient.openFoodFactsApi.getProductByBarcode(barcode)
            if (resp.status == 1) resp.product?.toNutritionResult() else null
        } catch (e: Exception) {
            null
        }
    }
}

fun SpoonacularMenuItem.toNutritionResult(): NutritionResult? {
    val cal = nutrition?.calories ?: return null
    // Spoonacular menu items are reported per-serving, not per-100g - we treat the serving as
    // "100g" internally so the app's existing gram-scaling still works; the quantity label shown
    // to the user says "1 serving" rather than grams for these results.
    return NutritionResult(
        name = title ?: "Menu item",
        brand = restaurantChain,
        caloriesPer100g = cal,
        proteinPer100g = nutrition.protein ?: 0.0,
        carbsPer100g = nutrition.carbs ?: 0.0,
        sugarPer100g = 0.0,
        fatPer100g = nutrition.fat ?: 0.0,
        fiberPer100g = 0.0,
        sodiumMgPer100g = 0.0,
        source = "Restaurant menu (Spoonacular)${restaurantChain?.let { " - $it" } ?: ""}",
        badge = "restaurant"
    )
}
