package com.dietary.tracker.network

import com.dietary.tracker.BuildConfig

object ApiKeys {
    val edamamAppId: String get() = BuildConfig.EDAMAM_APP_ID
    val edamamAppKey: String get() = BuildConfig.EDAMAM_APP_KEY
    val spoonacularKey: String get() = BuildConfig.SPOONACULAR_API_KEY
    val geminiKey: String get() = BuildConfig.GEMINI_API_KEY

    val hasEdamam: Boolean get() = edamamAppId.isNotBlank() && edamamAppKey.isNotBlank()
    val hasSpoonacular: Boolean get() = spoonacularKey.isNotBlank()
    val hasGemini: Boolean get() = geminiKey.isNotBlank()
}
