package com.example.nutriwise.data

import android.util.Log
import com.example.nutriwise.domain.HealthAlternative
import com.example.nutriwise.domain.MacroComparison
import com.example.nutriwise.domain.NutritionFact
import com.example.nutriwise.domain.ScoreFactor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DynamicAiResult(
    val productName: String,
    val score: Int,
    val verdict: String,
    val summary: String,
    val scoreAuditReason: String,
    val scannedMacroSummary: String,
    val nutritionTable: List<NutritionFact>,
    val scoreFactors: List<ScoreFactor>,
    val fullIngredientsList: List<String>,
    val simplifiedIngredients: List<String>,
    val healthBenefits: List<String>,
    val warnings: List<Pair<String, String>>,
    val containsPalmOil: Boolean,
    val palmOilDetails: String?,
    val dynamicAlternatives: List<HealthAlternative>
)

class AiExplanationService(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(35, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    private val supportedModels = listOf(
        "llama-3.1-8b-instant",
        "openai/gpt-oss-120b",
        "openai/gpt-oss-20b"
    )

    suspend fun analyzeLabelDynamically(scannedText: String, detectedBrandHint: String? = null): DynamicAiResult = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are an authoritative, clinical food toxicologist, dietary forensic auditor, and regulatory specialist in Indian FMCG packaged foods and FSSAI standards.
            Operate dynamically: clean and tokenize noisy OCR text, extract accurate nutrition facts, decode additives, and dynamically recommend 2 real, cleaner-label Indian packaged food alternatives in the exact same snack category available on quick-commerce apps (Blinkit, Zepto, Swiggy Instamart).
        """.trimIndent()

        val userPrompt = """
            INPUT SCANNED OCR & CONTEXT:
            \"\"\"
            $scannedText
            ${if (!detectedBrandHint.isNullOrBlank()) "VERIFIED REFERENCE: $detectedBrandHint" else ""}
            \"\"\"

            EXECUTE DYNAMIC FORENSIC ANALYSIS:
            1. Clean & Tokenize Ingredients: Split every ingredient into an individual array item with all declared percentages preserved.
            2. Standard Nutrition Panel per 100g: Energy (kcal), Carbohydrates (g), Added Sugars (g), Total Fat (g), Saturated Fat (g), Trans Fat (g), Protein (g), Sodium (mg) with Status ("Low", "Moderate", "High").
            3. Decode Additives: Decode all detected INS numbers into objective functional roles.
            4. Auditable NutriWise Score (15-95) with 5 Score Factors (Processing Degree, Added Sugars, Saturated Fat, Sodium Load, Additive Load).
            5. DYNAMIC SAME-CATEGORY SWAPS (2 items):
               - Suggest 2 real, commercially available cleaner Indian branded packaged products in the EXACT same category (e.g. if tea biscuits -> cleaner whole grain/millet biscuits; if spicy fried crisps -> roasted makhana or popped chips; if instant noodles -> millet/air-dried noodles).
               - Provide health score out of 100 (75-95).
               - Give a clean, short search keyword for quick commerce search bars.
               - Provide a head-to-head comparison between scanned product and the suggested swap.

            Respond STRICTLY in valid JSON matching this schema:
            {
              "productName": "<Brand + Full Variant Name>",
              "score": <Calculated Integer 15-95>,
              "verdict": "<Nutritious Choice | Moderate / Occasional Choice | Ultra-Processed / Consume Sparingly>",
              "summary": "<Objective 2-sentence clinical assessment>",
              "scoreAuditReason": "<1-sentence breakdown of why this score was calculated>",
              "scannedMacroSummary": "<e.g. 74% Refined Maida + Palm Oil>",
              "nutritionTable": [
                {"nutrientName": "Energy", "amountPer100g": "450 kcal", "status": "Moderate"},
                {"nutrientName": "Carbohydrates", "amountPer100g": "78 g", "status": "High"},
                {"nutrientName": "Added Sugars", "amountPer100g": "21.5 g", "status": "High"},
                {"nutrientName": "Total Fat", "amountPer100g": "11.5 g", "status": "Moderate"},
                {"nutrientName": "Saturated Fat", "amountPer100g": "5.2 g", "status": "Moderate"},
                {"nutrientName": "Trans Fat", "amountPer100g": "0 g", "status": "Low"},
                {"nutrientName": "Protein", "amountPer100g": "7.5 g", "status": "Moderate"},
                {"nutrientName": "Sodium", "amountPer100g": "290 mg", "status": "Low"}
              ],
              "scoreFactors": [
                {"factorName": "Processing Degree", "status": "<Status>", "isFavorable": <true/false>},
                {"factorName": "Added Sugars", "status": "<Status>", "isFavorable": <true/false>},
                {"factorName": "Saturated Fat", "status": "<Status>", "isFavorable": <true/false>},
                {"factorName": "Sodium Load", "status": "<Status>", "isFavorable": <true/false>},
                {"factorName": "Additive Load", "status": "<Status>", "isFavorable": <true/false>}
              ],
              "fullIngredientsList": [
                "<Individual Cleaned Ingredient with percentage if present>"
              ],
              "simplifiedIngredients": [
                "<INS Code and Name>: <Neutral functional explanation>"
              ],
              "containsPalmOil": <true/false>,
              "palmOilDetails": "<Factual oil note or null>",
              "healthBenefits": ["<Evidence-based fact or 'Provides quick dietary carbohydrates'>"],
              "warnings": [
                {"condition": "Nutrient Note: <Metric>", "message": "<Measured observation>"}
              ],
              "dynamicAlternatives": [
                {
                  "name": "<Real Indian Cleaner Product Name>",
                  "scoreOutOf100": <Integer 75-95>,
                  "whyBetterThanScanned": "<Direct head-to-head advantage>",
                  "cleanSearchQuery": "<Short search query for Blinkit/Zepto/Instamart>",
                  "alternativeSummary": "<e.g. 100% Whole Millets • 0% Palm Oil>",
                  "reason": "<Nutritional reason>"
                }
              ]
            }
        """.trimIndent()

        var lastError: Exception? = null

        for (modelId in supportedModels) {
            try {
                val requestJson = JSONObject().apply {
                    put("model", modelId)
                    put("response_format", JSONObject().put("type", "json_object"))
                    put("temperature", 0.1)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", userPrompt)
                        })
                    })
                }

                val request = Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.w("NutriWiseAI", "Model $modelId failed: $responseBody. Trying next...")
                    continue
                }

                val rootJson = JSONObject(responseBody)
                val rawContent = rootJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                val cleanJson = rawContent.substring(rawContent.indexOf('{'), rawContent.lastIndexOf('}') + 1)
                val jsonObj = JSONObject(cleanJson)

                val productName = jsonObj.optString("productName", "Scanned Food Product")
                val score = jsonObj.optInt("score", 45).coerceIn(15, 95)
                val verdict = jsonObj.optString("verdict", "Moderate / Occasional Choice")
                val summary = jsonObj.optString("summary", "Product evaluated.")
                val auditReason = jsonObj.optString("scoreAuditReason", "Score evaluated based on ingredient processing.")
                val macroSummary = jsonObj.optString("scannedMacroSummary", "Refined Ingredients")
                val containsPalm = jsonObj.optBoolean("containsPalmOil", false)
                val palmDetails = if (jsonObj.isNull("palmOilDetails")) null else jsonObj.optString("palmOilDetails")

                // 1. Nutrition Table
                val nutritionList = mutableListOf<NutritionFact>()
                val nutArray = jsonObj.optJSONArray("nutritionTable")
                if (nutArray != null) {
                    for (i in 0 until nutArray.length()) {
                        val nObj = nutArray.getJSONObject(i)
                        nutritionList.add(
                            NutritionFact(
                                nutrientName = nObj.optString("nutrientName"),
                                amountPer100g = nObj.optString("amountPer100g"),
                                status = nObj.optString("status")
                            )
                        )
                    }
                }

                // 2. Score Factors
                val factorsList = mutableListOf<ScoreFactor>()
                val factorsArray = jsonObj.optJSONArray("scoreFactors")
                if (factorsArray != null) {
                    for (i in 0 until factorsArray.length()) {
                        val fObj = factorsArray.getJSONObject(i)
                        factorsList.add(
                            ScoreFactor(
                                factorName = fObj.optString("factorName", "Factor"),
                                status = fObj.optString("status", "Moderate"),
                                isFavorable = fObj.optBoolean("isFavorable", false)
                            )
                        )
                    }
                }

                // 3. Full Ingredients
                val fullIngList = mutableListOf<String>()
                val fullIngArray = jsonObj.optJSONArray("fullIngredientsList")
                if (fullIngArray != null) {
                    for (i in 0 until fullIngArray.length()) {
                        fullIngList.add(fullIngArray.getString(i))
                    }
                }

                // 4. Simplified Additives
                val simplifiedList = mutableListOf<String>()
                val simplifiedArray = jsonObj.optJSONArray("simplifiedIngredients")
                if (simplifiedArray != null) {
                    for (i in 0 until simplifiedArray.length()) {
                        val rawText = simplifiedArray.getString(i)
                        val parts = rawText.split(":", limit = 2)
                        val formatted = if (parts.size == 2 && parts[0].contains("INS", ignoreCase = true)) {
                            InsAdditiveDatabase.decodeAdditive(parts[0])
                        } else {
                            rawText
                        }
                        simplifiedList.add(formatted)
                    }
                }

                // 5. Health Benefits
                val benefitsList = mutableListOf<String>()
                val benefitsArray = jsonObj.optJSONArray("healthBenefits")
                if (benefitsArray != null) {
                    for (i in 0 until benefitsArray.length()) {
                        benefitsList.add(benefitsArray.getString(i))
                    }
                }

                // 6. Warnings
                val warningsList = mutableListOf<Pair<String, String>>()
                val warningsArray = jsonObj.optJSONArray("warnings")
                if (warningsArray != null) {
                    for (i in 0 until warningsArray.length()) {
                        val item = warningsArray.getJSONObject(i)
                        warningsList.add(item.getString("condition") to item.getString("message"))
                    }
                }

                // 7. Dynamic Alternatives
                val dynamicAltsList = mutableListOf<HealthAlternative>()
                val altsArray = jsonObj.optJSONArray("dynamicAlternatives")
                if (altsArray != null) {
                    for (i in 0 until altsArray.length()) {
                        val altObj = altsArray.getJSONObject(i)
                        val altName = altObj.optString("name", "Healthy Alternative")
                        val altScore = altObj.optInt("scoreOutOf100", 85)
                        val whyBetter = altObj.optString("whyBetterThanScanned", "Cleaner ingredients and zero palm oil.")
                        val cleanQuery = altObj.optString("cleanSearchQuery", altName)
                        val altSummary = altObj.optString("alternativeSummary", "Whole Food Ingredients")

                        dynamicAltsList.add(
                            HealthAlternative(
                                name = altName,
                                estimatedPrice = null,
                                scoreOutOf100 = altScore,
                                whyBetterThanScanned = whyBetter,
                                cleanSearchQuery = cleanQuery,
                                imageUrl = null, // Will be fetched dynamically by DynamicProductImageService
                                availableOn = listOf("Blinkit", "Zepto", "Instamart"),
                                macroComparison = MacroComparison(
                                    scannedSummary = macroSummary,
                                    alternativeSummary = altSummary
                                ),
                                reason = altObj.optString("reason", "Superior nutrient profile.")
                            )
                        )
                    }
                }

                return@withContext DynamicAiResult(
                    productName = productName,
                    score = score,
                    verdict = verdict,
                    summary = summary,
                    scoreAuditReason = auditReason,
                    scannedMacroSummary = macroSummary,
                    nutritionTable = nutritionList,
                    scoreFactors = factorsList,
                    fullIngredientsList = fullIngList,
                    simplifiedIngredients = simplifiedList,
                    healthBenefits = benefitsList,
                    warnings = warningsList,
                    containsPalmOil = containsPalm,
                    palmOilDetails = palmDetails,
                    dynamicAlternatives = dynamicAltsList
                )
            } catch (e: Exception) {
                lastError = e
                Log.w("NutriWiseAI", "Model $modelId threw exception: ${e.message}")
            }
        }

        throw IllegalStateException("All models failed: ${lastError?.message}")
    }
}