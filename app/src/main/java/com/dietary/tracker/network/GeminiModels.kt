package com.dietary.tracker.network

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    @SerializedName("contents") val contents: List<GeminiContent>,
    @SerializedName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiContent(
    @SerializedName("parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @SerializedName("text") val text: String
)

data class GeminiGenerationConfig(
    @SerializedName("temperature") val temperature: Double = 0.2,
    @SerializedName("responseMimeType") val responseMimeType: String = "application/json"
)

data class GeminiResponse(
    @SerializedName("candidates") val candidates: List<GeminiCandidate>? = null
)

data class GeminiCandidate(
    @SerializedName("content") val content: GeminiContent? = null
)

/** Shape we ask Gemini to return, parsed from its JSON text response. */
data class AiNutritionJson(
    @SerializedName("name") val name: String? = null,
    @SerializedName("calories_per_100g") val calories: Double? = null,
    @SerializedName("protein_g_per_100g") val protein: Double? = null,
    @SerializedName("carbs_g_per_100g") val carbs: Double? = null,
    @SerializedName("sugar_g_per_100g") val sugar: Double? = null,
    @SerializedName("fat_g_per_100g") val fat: Double? = null,
    @SerializedName("fiber_g_per_100g") val fiber: Double? = null,
    @SerializedName("sodium_mg_per_100g") val sodium: Double? = null
)
