package com.example.nutriwise.data

import android.util.Log
import com.example.nutriwise.domain.DynamicHealthWarning
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
    val aiExplanation: String,
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
    val dynamicAlternatives: List<HealthAlternative>,
    val personalizedWarnings: List<DynamicHealthWarning>
)

class AiExplanationService(private val apiKey: String) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeLabelDynamically(
        scannedText: String,
        detectedBrandHint: String? = null,
        userConditions: List<String> = emptyList()
    ): DynamicAiResult = withContext(Dispatchers.IO) {

        val trimmedKey = apiKey.trim()

        // Google Gemini keys (both modern "AQ." and legacy "AIzaSy" formats)
        if (trimmedKey.startsWith("AQ.") || trimmedKey.startsWith("AIzaSy")) {
            return@withContext callGeminiApi(trimmedKey, scannedText, detectedBrandHint, userConditions)
        }

        // Groq, Cerebras, xAI OpenAI-compatible fallback
        return@withContext callOpenAiCompatibleApi(trimmedKey, scannedText, detectedBrandHint, userConditions)
    }

    private fun callGeminiApi(
        key: String,
        scannedText: String,
        detectedBrandHint: String?,
        userConditions: List<String>
    ): DynamicAiResult {
        val conditionsText = if (userConditions.isNotEmpty()) {
            userConditions.joinToString(", ")
        } else {
            "None specified (general public health evaluation)"
        }
        val prompt = """
            You are an authoritative clinical food forensic auditor and regulatory specialist in FSSAI and Indian packaged foods.
            
            OBJECTIVE:
            Extract or calculate complete nutritional facts and evaluate the scanned food item.
            
            DATA CONTEXT:
            Scanned OCR Text:
            \"\"\"
            $scannedText
            \"\"\"
            Verified Brand/Product Hint: ${detectedBrandHint ?: "Indian packaged snack/food"}
            User Specific Profile: [$conditionsText]

            CRITICAL NUTRITION EXTRACTION RULES:
            1. Extract exact per 100g numbers from the scan if visible.
            2. If an essential metric (Energy, Carbs, Added Sugars, Total Fat, Saturated Fat, Trans Fat, Protein, Sodium) is partly cropped or obscured by OCR, USE YOUR VERIFIED DATABASE KNOWLEDGE of this exact Indian product/category to provide accurate standard values per 100g.
            3. DO NOT output "Not Declared" unless the nutrient is genuinely inapplicable (e.g. Sodium in table sugar). Always provide realistic, standard values with proper units (e.g., "520 kcal", "68g", "22g", "120mg").
            4. Assign an accurate status ("Low", "Moderate", "High") according to standard WHO guidelines.
            5. Decode all INS/E-number additives into plain English functional explanations.
            6. Suggest 2 real, cleaner packaged alternatives sold on Indian quick-commerce (Blinkit, Zepto, Swiggy Instamart).

            Output MUST be valid JSON matching this schema:
            {
              "productName": "<Exact brand and product name>",
              "score": 45,
              "verdict": "<Nutritious Choice | Moderate / Occasional Choice | Ultra-Processed / Consume Sparingly>",
              "summary": "<Objective 2-sentence clinical assessment>",
              "aiExplanation": "<2-3 sentence personalized verdict addressing user's declared profile>",
              "scoreAuditReason": "<1-sentence breakdown of why this score was calculated>",
              "scannedMacroSummary": "<e.g. 74% Refined Maida + Palm Oil>",
              "nutritionTable": [
                {"nutrientName": "Energy", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Carbohydrates", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Added Sugars", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Total Fat", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Saturated Fat", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Trans Fat", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Protein", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"},
                {"nutrientName": "Sodium", "amountPer100g": "<Value with unit>", "status": "<Low|Moderate|High>"}
              ],
              "scoreFactors": [
                {"factorName": "Processing Degree", "status": "High", "isFavorable": false},
                {"factorName": "Added Sugars", "status": "Moderate", "isFavorable": false},
                {"factorName": "Saturated Fat", "status": "High", "isFavorable": false},
                {"factorName": "Sodium Load", "status": "Moderate", "isFavorable": true},
                {"factorName": "Additive Load", "status": "High", "isFavorable": false}
              ],
              "fullIngredientsList": [
                "<Individual ingredient names cleaned from OCR>"
              ],
              "simplifiedIngredients": [
                "<INS Code and Name>: <Neutral functional explanation>"
              ],
              "containsPalmOil": false,
              "palmOilDetails": "<Factual oil note or null>",
              "healthBenefits": ["<Evidence-based health benefit>"],
              "warnings": [
                {"condition": "Nutrient Note", "message": "<Clinical observation>"}
              ],
              "personalizedWarnings": [
                {
                  "condition": "<User Condition Name>",
                  "severity": "CRITICAL",
                  "reason": "<Specific reason why this product affects the condition>"
                }
              ],
              "dynamicAlternatives": [
                {
                  "name": "<Real cleaner Indian alternative brand and product>",
                  "scoreOutOf100": 85,
                  "whyBetterThanScanned": "<Direct comparative nutritional advantage>",
                  "cleanSearchQuery": "<Precise search query for Blinkit/Zepto/Instamart>",
                  "alternativeSummary": "<e.g. 100% Whole Wheat • Zero Palm Oil>",
                  "reason": "<Reason for recommendation>"
                }
              ]
            }
        """.trimIndent()

        // Modern Gemini 2.0 / 2.5 flash models

        var lastError: Exception? = null
        val candidateModels = listOf(
            "gemini-3.6-flash",
            "gemini-2.5-flash"
        )
        for (modelName in candidateModels) {
            try {
                val payload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.2)
                    })
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key")
                    .addHeader("x-goog-api-key", key) // Required for AQ. authentication keys
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val err = "Gemini API Error ($modelName ${response.code}):$responseBody"
                    Log.w("NutriWiseAI", err)
                    lastError = Exception(err)
                    continue
                }

                val root = JSONObject(responseBody)
                val candidates = root.optJSONArray("candidates") ?: throw Exception("No candidates returned from Gemini")
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val text = content.getJSONArray("parts").getJSONObject(0).getString("text")

                return parseCleanJson(text, detectedBrandHint)
            } catch (e: Exception) {
                lastError = e
                Log.w("NutriWiseAI", "Model $modelName call failed:${e.message}")
            }
        }

        throw IllegalStateException("Gemini Analysis failed: ${lastError?.message}")
    }

    private fun callOpenAiCompatibleApi(
        key: String,
        scannedText: String,
        detectedBrandHint: String?,
        userConditions: List<String>
    ): DynamicAiResult {
        val url = when {
            key.startsWith("gsk_") -> "https://api.groq.com/openai/v1/chat/completions"
            key.startsWith("xai-") -> "https://api.x.ai/v1/chat/completions"
            key.startsWith("csk-") -> "https://api.cerebras.ai/v1/chat/completions"
            else -> "https://api.groq.com/openai/v1/chat/completions"
        }

        val model = when {
            key.startsWith("gsk_") -> "llama-3.3-70b-versatile"
            key.startsWith("xai-") -> "grok-2"
            key.startsWith("csk-") -> "llama3.1-8b"
            else -> "llama-3.3-70b-versatile"
        }

        val conditionsText = if (userConditions.isNotEmpty()) userConditions.joinToString(", ") else "None"
        val prompt = "Extract nutritional facts and evaluate for: $conditionsText. Scanned text:\n$scannedText"

        val requestJson = JSONObject().apply {
            put("model", model)
            put("temperature", 0.0)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = httpClient.newCall(request).execute()
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) throw IllegalStateException("API Error: $body")

        val raw = JSONObject(body).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
        return parseCleanJson(raw, detectedBrandHint)
    }

    private fun parseCleanJson(rawContent: String, detectedBrandHint: String?): DynamicAiResult {
        val startIdx = rawContent.indexOf('{')
        val endIdx = rawContent.lastIndexOf('}')
        val cleanJson = if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            rawContent.substring(startIdx, endIdx + 1)
        } else {
            rawContent.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        val jsonObj = JSONObject(cleanJson)

        val productName = jsonObj.optString("productName", detectedBrandHint ?: "Scanned Food Product")
        val score = jsonObj.optInt("score", 45).coerceIn(15, 95)
        val verdict = jsonObj.optString("verdict", "Moderate / Occasional Choice")
        val summary = jsonObj.optString("summary", "Product evaluated.")
        val aiExplanation = jsonObj.optString("aiExplanation", "Nutritional evaluation completed.")
        val auditReason = jsonObj.optString("scoreAuditReason", "Score evaluated based on ingredient processing.")
        val macroSummary = jsonObj.optString("scannedMacroSummary", "Refined Ingredients")
        val containsPalm = jsonObj.optBoolean("containsPalmOil", false)
        val palmDetails = if (jsonObj.isNull("palmOilDetails")) null else jsonObj.optString("palmOilDetails")

        val nutritionList = mutableListOf<NutritionFact>()
        jsonObj.optJSONArray("nutritionTable")?.let { arr ->
            for (i in 0 until arr.length()) {
                val nObj = arr.getJSONObject(i)
                nutritionList.add(
                    NutritionFact(
                        nutrientName = nObj.optString("nutrientName"),
                        amountPer100g = nObj.optString("amountPer100g"),
                        status = nObj.optString("status")
                    )
                )
            }
        }

        val factorsList = mutableListOf<ScoreFactor>()
        jsonObj.optJSONArray("scoreFactors")?.let { arr ->
            for (i in 0 until arr.length()) {
                val fObj = arr.getJSONObject(i)
                factorsList.add(
                    ScoreFactor(
                        factorName = fObj.optString("factorName", "Factor"),
                        status = fObj.optString("status", "Moderate"),
                        isFavorable = fObj.optBoolean("isFavorable", false)
                    )
                )
            }
        }

        val fullIngList = mutableListOf<String>()
        jsonObj.optJSONArray("fullIngredientsList")?.let { arr ->
            for (i in 0 until arr.length()) fullIngList.add(arr.getString(i))
        }

        val simplifiedList = mutableListOf<String>()
        jsonObj.optJSONArray("simplifiedIngredients")?.let { arr ->
            for (i in 0 until arr.length()) simplifiedList.add(arr.getString(i))
        }

        val benefitsList = mutableListOf<String>()
        jsonObj.optJSONArray("healthBenefits")?.let { arr ->
            for (i in 0 until arr.length()) benefitsList.add(arr.getString(i))
        }

        val warningsList = mutableListOf<Pair<String, String>>()
        jsonObj.optJSONArray("warnings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                warningsList.add(item.getString("condition") to item.getString("message"))
            }
        }

        val personalizedWarningsList = mutableListOf<DynamicHealthWarning>()
        jsonObj.optJSONArray("personalizedWarnings")?.let { arr ->
            for (i in 0 until arr.length()) {
                val pObj = arr.getJSONObject(i)
                personalizedWarningsList.add(
                    DynamicHealthWarning(
                        condition = pObj.optString("condition", "Health Note"),
                        severity = pObj.optString("severity", "MODERATE"),
                        reason = pObj.optString("reason", "")
                    )
                )
            }
        }

        val dynamicAltsList = mutableListOf<HealthAlternative>()
        jsonObj.optJSONArray("dynamicAlternatives")?.let { arr ->
            for (i in 0 until arr.length()) {
                val altObj = arr.getJSONObject(i)
                dynamicAltsList.add(
                    HealthAlternative(
                        name = altObj.optString("name", "Healthy Alternative"),
                        estimatedPrice = null,
                        scoreOutOf100 = altObj.optInt("scoreOutOf100", 85),
                        whyBetterThanScanned = altObj.optString("whyBetterThanScanned", "Cleaner ingredients."),
                        cleanSearchQuery = altObj.optString("cleanSearchQuery", altObj.optString("name")),
                        imageUrl = null,
                        availableOn = listOf("Blinkit", "Zepto", "Instamart"),
                        macroComparison = MacroComparison(
                            scannedSummary = macroSummary,
                            alternativeSummary = altObj.optString("alternativeSummary", "Whole Food Ingredients")
                        ),
                        reason = altObj.optString("reason", "Superior nutrient profile.")
                    )
                )
            }
        }

        return DynamicAiResult(
            productName = productName,
            score = score,
            verdict = verdict,
            summary = summary,
            aiExplanation = aiExplanation,
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
            dynamicAlternatives = dynamicAltsList,
            personalizedWarnings = personalizedWarningsList
        )
    }
}