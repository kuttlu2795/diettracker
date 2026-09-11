package com.dietary.tracker.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import com.dietary.tracker.network.NutritionResult

object NutritionLabelParser {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Runs OCR on the bitmap, then parses common nutrition-label wording into a NutritionResult. */
    suspend fun parse(bitmap: Bitmap, fallbackName: String = "Scanned Label"): NutritionResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        val text = result.text
        return parseText(text, fallbackName)
    }

    fun parseText(text: String, fallbackName: String = "Scanned Label"): NutritionResult {
        val lower = text.lowercase()

        fun find(vararg keys: String): Double {
            for (key in keys) {
                val regex = Regex("$key[^0-9]{0,15}([0-9]+[.,]?[0-9]*)")
                val match = regex.find(lower)
                if (match != null) {
                    val raw = match.groupValues[1].replace(",", ".")
                    return raw.toDoubleOrNull() ?: 0.0
                }
            }
            return 0.0
        }

        val calories = find("energy.*kcal", "calories", "energy")
        val protein = find("protein")
        val carbs = find("carbohydrate", "total carb")
        val sugar = find("sugars", "sugar")
        val fat = find("total fat", "fat")
        val fiber = find("fibre", "fiber", "dietary fiber")
        val sodium = find("sodium", "salt") * (if (lower.contains("salt")) 400.0 else 1.0)

        return NutritionResult(
            name = fallbackName,
            caloriesPer100g = if (calories > 0) calories else 100.0,
            proteinPer100g = protein,
            carbsPer100g = carbs,
            sugarPer100g = sugar,
            fatPer100g = fat,
            fiberPer100g = fiber,
            sodiumMgPer100g = sodium,
            source = "Photo OCR (verify values)",
            badge = "manual"
        )
    }
}
