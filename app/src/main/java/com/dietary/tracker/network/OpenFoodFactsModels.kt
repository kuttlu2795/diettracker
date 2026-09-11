package com.dietary.tracker.network

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("products") val products: List<Product>? = null
)

data class ProductResponse(
    @SerializedName("status") val status: Int? = null,
    @SerializedName("product") val product: Product? = null
)

data class Product(
    @SerializedName("code") val code: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    @SerializedName("brands") val brands: String? = null,
    @SerializedName("image_front_small_url") val imageUrl: String? = null,
    @SerializedName("quantity") val quantity: String? = null,
    @SerializedName("nutriments") val nutriments: Nutriments? = null
)

data class Nutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerializedName("proteins_100g") val proteins100g: Double? = null,
    @SerializedName("carbohydrates_100g") val carbs100g: Double? = null,
    @SerializedName("sugars_100g") val sugars100g: Double? = null,
    @SerializedName("fat_100g") val fat100g: Double? = null,
    @SerializedName("fiber_100g") val fiber100g: Double? = null,
    @SerializedName("sodium_100g") val sodium100g: Double? = null // grams per 100g in OFF
)
