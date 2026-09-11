package com.example.nutriwise.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ProductResponseDto(
    @Json(name = "status") val status: Int? = 0,
    @Json(name = "product") val product: ProductDto? = null
)

@JsonClass(generateAdapter = true)
data class ProductDto(
    @Json(name = "code") val code: String? = null,
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null,
    @Json(name = "nutriments") val nutriments: NutrimentsDto? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = emptyList(),
    @Json(name = "image_url") val imageUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class NutrimentsDto(
    @Json(name = "sugars_100g") val sugars100g: Double? = 0.0,
    @Json(name = "sodium_100g") val sodium100g: Double? = 0.0,
    @Json(name = "saturated-fat_100g") val saturatedFat100g: Double? = 0.0,
    @Json(name = "proteins_100g") val proteins100g: Double? = 0.0,
    @Json(name = "fiber_100g") val fiber100g: Double? = 0.0,
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = 0.0
)