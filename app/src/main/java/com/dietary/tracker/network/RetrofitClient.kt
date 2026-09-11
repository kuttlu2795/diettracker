package com.dietary.tracker.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun retrofitFor(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /** Free, keyless - packaged/branded product lookups (search + barcode). */
    val openFoodFactsApi: OpenFoodFactsApi by lazy {
        retrofitFor("https://world.openfoodfacts.org/").create(OpenFoodFactsApi::class.java)
    }

    /** Requires a free Edamam app id/key (see local.properties.example) - natural-language nutrition analysis. */
    val edamamApi: EdamamApi by lazy {
        retrofitFor("https://api.edamam.com/").create(EdamamApi::class.java)
    }

    /** Requires a free Spoonacular API key (see local.properties.example) - AI-assisted daily meal plans. */
    val spoonacularApi: SpoonacularApi by lazy {
        retrofitFor("https://api.spoonacular.com/").create(SpoonacularApi::class.java)
    }

    /** Requires a free Google Gemini API key (see local.properties.example) - AI fallback nutrition estimates. */
    val geminiApi: GeminiApi by lazy {
        retrofitFor("https://generativelanguage.googleapis.com/").create(GeminiApi::class.java)
    }

    // Kept for backwards-compat with older references.
    val api: OpenFoodFactsApi get() = openFoodFactsApi
}
