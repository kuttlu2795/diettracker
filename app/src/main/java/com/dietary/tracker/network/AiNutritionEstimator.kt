package com.dietary.tracker.network

import com.google.gson.Gson

sealed class AiEstimateOutcome {
    data class Success(val result: NutritionResult) : AiEstimateOutcome()
    data class Error(val message: String) : AiEstimateOutcome()
    object MissingApiKey : AiEstimateOutcome()
}

/**
 * Fallback used only when Open Food Facts + Edamam have nothing for a food description.
 * Sends the exact text the user typed (or the description built by a quick-add customizer,
 * e.g. "3 eggs, without yolk, no salt, with pepper") to Gemini and asks for a strict JSON
 * nutrition estimate per 100g. Nothing about the numbers is hardcoded - they come back from
 * the live model call each time.
 */
class AiNutritionEstimator {

    private val gson = Gson()

    suspend fun estimate(description: String): AiEstimateOutcome {
        if (!ApiKeys.hasGemini) return AiEstimateOutcome.MissingApiKey

        val prompt = """
            You are a nutrition estimation assistant. Estimate the nutrition facts for this food,
            per 100g (or per 100ml for drinks): "$description"

            Respond with ONLY a single JSON object, no markdown, no explanation, in exactly this shape:
            {"name":"<short food name>","calories_per_100g":<number>,"protein_g_per_100g":<number>,
             "carbs_g_per_100g":<number>,"sugar_g_per_100g":<number>,"fat_g_per_100g":<number>,
             "fiber_g_per_100g":<number>,"sodium_mg_per_100g":<number>}

            Use realistic average values. All numbers must be plain numbers (no units, no ranges).
        """.trimIndent()

        return try {
            val response = RetrofitClient.geminiApi.generateContent(
                apiKey = ApiKeys.geminiKey,
                request = GeminiRequest(
                    contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
                    generationConfig = GeminiGenerationConfig()
                )
            )
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return AiEstimateOutcome.Error("AI returned an empty response.")

            val cleaned = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = gson.fromJson(cleaned, AiNutritionJson::class.java)
                ?: return AiEstimateOutcome.Error("Couldn't parse the AI's response.")

            if (parsed.calories == null) {
                return AiEstimateOutcome.Error("The AI response didn't include calorie data.")
            }

            AiEstimateOutcome.Success(
                NutritionResult(
                    name = parsed.name?.takeIf { it.isNotBlank() }
                        ?: description.replaceFirstChar { it.uppercase() },
                    caloriesPer100g = parsed.calories,
                    proteinPer100g = parsed.protein ?: 0.0,
                    carbsPer100g = parsed.carbs ?: 0.0,
                    sugarPer100g = parsed.sugar ?: 0.0,
                    fatPer100g = parsed.fat ?: 0.0,
                    fiberPer100g = parsed.fiber ?: 0.0,
                    sodiumMgPer100g = parsed.sodium ?: 0.0,
                    source = "AI estimate (Gemini) - verify before saving",
                    badge = "ai"
                )
            )
        } catch (e: Exception) {
            AiEstimateOutcome.Error(e.message ?: "AI lookup failed. Check your connection.")
        }
    }

    /**
     * Meal-photo recognition: sends a photo of a plate of food to Gemini's vision model and asks
     * it to identify what's on the plate and estimate total nutrition for the portion shown.
     * Unlike the label-OCR flow (which reads printed numbers off a package), this asks the model
     * to actually recognize the food itself from the image - no nutrition text needs to be visible.
     */
    suspend fun estimateFromImage(bitmap: android.graphics.Bitmap): AiEstimateOutcome {
        if (!ApiKeys.hasGemini) return AiEstimateOutcome.MissingApiKey

        val base64 = try {
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, stream)
            android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            return AiEstimateOutcome.Error("Couldn't process the photo.")
        }

        val prompt = """
            Identify the food/meal shown in this photo and estimate its total nutrition for the
            portion visible (not per 100g - the whole plate/portion shown).

            Respond with ONLY a single JSON object, no markdown, no explanation, in exactly this
            shape (the numbers are for the WHOLE portion shown, not per 100g):
            {"name":"<short description of the meal>","calories_per_100g":<total calories>,
             "protein_g_per_100g":<total protein g>,"carbs_g_per_100g":<total carbs g>,
             "sugar_g_per_100g":<total sugar g>,"fat_g_per_100g":<total fat g>,
             "fiber_g_per_100g":<total fiber g>,"sodium_mg_per_100g":<total sodium mg>}

            (Field names say "per_100g" for compatibility but here they mean totals for the
            portion shown - do not divide by 100g.) Use realistic average values for a typical
            serving of what's visible. All numbers must be plain numbers.
        """.trimIndent()

        return try {
            val response = RetrofitClient.geminiApi.generateContent(
                apiKey = ApiKeys.geminiKey,
                request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = prompt),
                                GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64))
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig()
                )
            )
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return AiEstimateOutcome.Error("AI returned an empty response.")
            val cleaned = rawText.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val parsed = gson.fromJson(cleaned, AiNutritionJson::class.java)
                ?: return AiEstimateOutcome.Error("Couldn't parse the AI's response.")
            if (parsed.calories == null) {
                return AiEstimateOutcome.Error("Couldn't identify nutrition from that photo. Try a clearer shot.")
            }

            // Values here are already totals for the portion, not per-100g. We store them as if
            // "100g = the whole portion" so the app's existing scaling math (grams / 100) keeps
            // working unchanged if the user later tweaks the quantity field.
            AiEstimateOutcome.Success(
                NutritionResult(
                    name = parsed.name?.takeIf { it.isNotBlank() } ?: "Meal photo",
                    caloriesPer100g = parsed.calories,
                    proteinPer100g = parsed.protein ?: 0.0,
                    carbsPer100g = parsed.carbs ?: 0.0,
                    sugarPer100g = parsed.sugar ?: 0.0,
                    fatPer100g = parsed.fat ?: 0.0,
                    fiberPer100g = parsed.fiber ?: 0.0,
                    sodiumMgPer100g = parsed.sodium ?: 0.0,
                    source = "AI meal photo recognition (Gemini Vision) - verify before saving",
                    badge = "ai"
                )
            )
        } catch (e: Exception) {
            AiEstimateOutcome.Error(e.message ?: "AI photo lookup failed. Check your connection.")
        }
    }
}
