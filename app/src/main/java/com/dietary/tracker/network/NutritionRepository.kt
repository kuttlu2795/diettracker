package com.dietary.tracker.network

/**
 * All nutrition data in this app comes from live online sources - nothing is hardcoded:
 *  - Edamam Nutrition Analysis API: natural-language parsing ("1 medium banana", "100g grilled
 *    chicken breast") - great for fruits, home-cooked meals, generic ingredients.
 *  - Open Food Facts: free, keyless database of packaged/branded products - used for search
 *    and barcode scans.
 * Results from both are merged so the user sees the best available match.
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
                // Ignore - fall through to Open Food Facts below.
            }
        }

        try {
            val offResults = RetrofitClient.openFoodFactsApi.searchProducts(query).products
                ?.mapNotNull { it.toNutritionResult() } ?: emptyList()
            results.addAll(offResults)
        } catch (e: Exception) {
            // Ignore - network may be unavailable.
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
