package com.example.nutriwise.data

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlin.coroutines.resume

data class ScanDetectionResult(
    val barcode: String?,
    val verifiedProductName: String?,
    val verifiedBrand: String?,
    val verifiedImageUrl: String?,
    val ingredientsText: String
)

class DualScannerEngine {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient()
    private val httpClient = OkHttpClient()

    suspend fun analyzeImage(bitmap: Bitmap): ScanDetectionResult = withContext(Dispatchers.IO) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)

        // 1. Scan for Barcode
        val detectedBarcode = scanBarcode(inputImage)

        // 2. If barcode detected, fetch verified product metadata from Open Food Facts
        var verifiedName: String? = null
        var verifiedBrand: String? = null
        var verifiedImage: String? = null
        var verifiedIngredients: String? = null

        if (!detectedBarcode.isNullOrBlank()) {
            val fetchedData = fetchOpenFoodFactsData(detectedBarcode)
            verifiedName = fetchedData.first
            verifiedBrand = fetchedData.second
            verifiedImage = fetchedData.third
            verifiedIngredients = fetchedData.fourth
        }

        // 3. Perform OCR for ingredient list fallback/enrichment
        val ocrText = scanOcrText(inputImage)

        val combinedText = buildString {
            if (!verifiedIngredients.isNullOrBlank()) {
                append("Verified Product Ingredients: $verifiedIngredients\n")
            }
            if (!verifiedName.isNullOrBlank()) {
                append("Product Name: $verifiedName (Brand: $verifiedBrand)\n")
            }
            append("Scanned Label Text:\n$ocrText")
        }

        ScanDetectionResult(
            barcode = detectedBarcode,
            verifiedProductName = verifiedName,
            verifiedBrand = verifiedBrand,
            verifiedImageUrl = verifiedImage,
            ingredientsText = combinedText
        )
    }

    private suspend fun scanBarcode(image: InputImage): String? = suspendCancellableCoroutine { continuation ->
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                val code = barcodes.firstOrNull { it.format == Barcode.FORMAT_EAN_13 || it.format == Barcode.FORMAT_UPC_A || it.format == Barcode.FORMAT_EAN_8 }?.rawValue
                continuation.resume(code)
            }
            .addOnFailureListener {
                continuation.resume(null)
            }
    }

    private suspend fun scanOcrText(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                continuation.resume(visionText.text)
            }
            .addOnFailureListener {
                continuation.resume("")
            }
    }

    private fun fetchOpenFoodFactsData(barcode: String): Quadruple<String?, String?, String?, String?> {
        return try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name,brands,image_url,ingredients_text"
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "NutriWise-AndroidApp/2.0")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                if (json.optInt("status") == 1) {
                    val product = json.getJSONObject("product")
                    val name = product.optString("product_name").takeIf { it.isNotBlank() }
                    val brand = product.optString("brands").takeIf { it.isNotBlank() }
                    val img = product.optString("image_url").takeIf { it.isNotBlank() }
                    val ing = product.optString("ingredients_text").takeIf { it.isNotBlank() }
                    return Quadruple(name, brand, img, ing)
                }
            }
            Quadruple(null, null, null, null)
        } catch (e: Exception) {
            Quadruple(null, null, null, null)
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)