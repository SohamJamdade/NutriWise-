package com.example.nutriwise.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

class DynamicProductImageService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRealProductImage(productName: String): String? = withContext(Dispatchers.IO) {
        val queryList = listOf(
            productName,
            productName.split(" ").take(3).joinToString(" ") // Shortened query for broader matching
        ).distinct()

        for (query in queryList) {
            try {
                val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
                val url = "https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encodedQuery&search_simple=1&action=process&json=1&page_size=3"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "NutriWiseApp-Android/2.0")
                    .get()
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = JSONObject(body)
                    val products = json.optJSONArray("products")
                    if (products != null && products.length() > 0) {
                        for (i in 0 until products.length()) {
                            val prod = products.getJSONObject(i)
                            val imageUrl = prod.optString("image_front_url", "")
                                .ifBlank { prod.optString("image_url", "") }
                                .ifBlank { prod.optString("image_small_url", "") }

                            if (imageUrl.isNotBlank() && imageUrl.startsWith("http")) {
                                Log.d("NutriWiseImg", "Found image for '$query': $imageUrl")
                                return@withContext imageUrl
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("NutriWiseImg", "Image fetch error for '$query': ${e.message}")
            }
        }
        null
    }
}