package com.example.nutriwise.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "code,product_name,brands,ingredients_text,nutriments,allergens_tags,image_url"
    ): ProductResponseDto
}