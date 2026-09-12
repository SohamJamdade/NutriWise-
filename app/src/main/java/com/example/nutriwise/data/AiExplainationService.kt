package com.example.nutriwise.data

import android.util.Log
import com.example.nutriwise.domain.DynamicHealthWarning
import com.example.nutriwise.domain.HealthAlternative
import com.example.nutriwise.domain.MacroComparison
import com.example.nutriwise.domain.NutritionFact
import com.example.nutriwise.domain.ScoreFactor
import com.example.nutriwise.domain.UserProfile
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
        userConditions: List<String> = emptyList(),
        userProfile: UserProfile? = null
    ): DynamicAiResult = withContext(Dispatchers.IO) {

        val trimmedKey = apiKey.trim()

        if (trimmedKey.startsWith("AQ.") || trimmedKey.startsWith("AIzaSy")) {
            return@withContext callGeminiApi(trimmedKey, scannedText, detectedBrandHint, userConditions, userProfile)
        }

        return@withContext callOpenAiCompatibleApi(trimmedKey, scannedText, detectedBrandHint, userConditions, userProfile)
    }

    private fun callGeminiApi(
        key: String,
        scannedText: String,
        detectedBrandHint: String?,
        userConditions: List<String>,
        userProfile: UserProfile? = null
    ): DynamicAiResult {
        val conditionsText = userConditions.joinToString(", ").ifBlank { "None declared (general public health evaluation)" }
        val fitnessGoal = userProfile?.fitnessGoal?.ifBlank { "Maintain & Clean Eating" } ?: "Maintain & Clean Eating"
        val userWeightKg = userProfile?.weightKg?.let { "$it kg" } ?: "Not provided"
        val userHeightCm = userProfile?.heightCm?.let { "$it cm" } ?: "Not provided"
        val userAge = userProfile?.age?.let { "$it years" } ?: "Not provided"

        val prompt = """
    You are an authoritative clinical food forensic auditor, molecular biochemist, and sports nutritionist specializing in FSSAI regulations, WHO dietary guidelines, and Indian packaged foods.
    
    PRIMARY OBJECTIVE:
    Forensically audit the scanned product label against the user's specific health directives, chronic conditions, and metabolic targets. Reconstruct missing or obscured macro/micronutrient profiles using verified product intelligence.

    USER CLINICAL PROFILE & BIOMETRICS:
    - Declared Conditions & Directives: [$conditionsText]
    - Target Fitness Goal: $fitnessGoal
    - Age: $userAge
    - Body Weight: $userWeightKg
    - Height: $userHeightCm

    SCANNED OCR TEXT:
    \"\"\"
    $scannedText
    \"\"\"
    Verified Brand / Product Context Hint: ${detectedBrandHint ?: "Indian packaged snack/beverage/staple"}

    CLINICAL AUDIT DIRECTIVES & PATHOLOGY PROTOCOLS:
    1. PATHOLOGY CONTRAINDICATIONS:
       - If user lists Cancer or Oncology Directives: Scrutinize for synthetic food dyes (INS 102 Tartrazine, INS 110 Sunset Yellow, INS 129 Allura Red), artificial sweeteners (Aspartame INS 951, Acesulfame K INS 950, Sucralose INS 955), BHA (INS 320), BHT (INS 321), high fructose corn syrup, and oxidized high-heat seed oils.
       - If user lists Celiac / Gluten Sensitivity: Flag wheat, maida, barley, malt extract, semolina (sooji), rye, and ambiguous "modified food starch" or "stabilizers (INS 1422, 1442)" without explicit gluten-free declaration.
       - If user lists Guillain-Barré Syndrome (GBS) / Autoimmune / Neuropathy: Flag neurotoxic excitotoxins (Monosodium Glutamate INS 621, Disodium Guanylate INS 627, Inosinate INS 631), artificial sweeteners, and systemic inflammatory refined seed oils.
       - If user lists Diabetes / Pre-Diabetes / Insulin Resistance: Scrutinize glycemic load, maltodextrin (Glycemic Index 110-185), inverted sugar syrup, liquid glucose, and total carbohydrate-to-fiber ratio.
       - If user lists Hypertension / Renal Impairment / Kidney Disease: Strictly audit sodium (mg per 100g and per serving) and potassium additives (e.g., Potassium Chloride INS 508).
       - If user lists Fatty Liver / High Cholesterol / Cardiac Risk: Audit palm oil, palmolein, interesterified vegetable fats, hydrogenated oils, trans fatty acids, and saturated fat percentages.

    2. METABOLIC GOAL & ATHLETIC CALIBRATION:
       - Weight Gain / Muscle Building: Reward protein density (P:E ratio) and bioavailable calories. Tolerate complex fats/carbs, but heavily penalize empty sugars and trans fats.
       - Weight Loss / Fat Cut: Heavily penalize caloric density, refined carbohydrates (Maida), hidden liquid sugars, and saturated fat. Reward high dietary fiber (>6g/100g) and protein satiety.
       - Maintain & Clean Eating / Athletic Performance: Heavily penalize hyper-palatable industrial additives, artificial emulsifiers (INS 471, 472), and sodium imbalance. Reward clean electrolyte profiles and whole-food matrices.

    3. NUTRITION EXTRACTION & DATA RECONSTRUCTION RULES:
       - Extract exact per 100g metrics from OCR.
       - If an essential metric (Energy, Carbs, Added Sugars, Total Fat, Saturated Fat, Trans Fat, Protein, Sodium) is cropped, distorted, or missing from OCR, USE YOUR VERIFIED DATABASE KNOWLEDGE of this exact Indian product/category to provide accurate standard values per 100g.
       - DO NOT output "Not Declared" or "N/A" unless fundamentally non-applicable. Always supply units (e.g., "520 kcal", "68g", "22g", "120mg").
       - Assign status ("Low", "Moderate", "High") following WHO nutrient profiling guidelines.
       - Decode all INS / E-numbers into plain English with their functional biological mechanism.

    OUTPUT FORMAT:
    You MUST output valid, parseable JSON matching this EXACT structure with no preamble, no markdown formatting outside of JSON, and no code ticks:
    {
      "productName": "<Exact brand and product name>",
      "score": 45,
      "verdict": "<Nutritious Choice | Moderate / Occasional Choice | Ultra-Processed / Consume Sparingly>",
      "summary": "<Objective 2-sentence clinical assessment>",
      "aiExplanation": "<2-3 sentence personalized verdict addressing the user's specific clinical conditions, weight, and fitness goal>",
      "scoreAuditReason": "<1-sentence breakdown explaining the score calculation against their fitness goal and ingredient processing level>",
      "scannedMacroSummary": "<e.g., 68% Refined Wheat Flour + 24% Palm Oil>",
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
        {"factorName": "Goal Alignment", "status": "<Favorable|Unfavorable|Neutral>", "isFavorable": false},
        {"factorName": "Added Sugars", "status": "Moderate", "isFavorable": false},
        {"factorName": "Protein Efficiency", "status": "<Low|Moderate|High>", "isFavorable": true},
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
      "healthBenefits": ["<Evidence-based health benefit or null if ultra-processed>"],
      "warnings": [
        {"condition": "Nutrient Directive", "message": "<Clinical observation>"}
      ],
      "personalizedWarnings": [
        {
          "condition": "<Specific Condition or Fitness Goal>",
          "severity": "<CRITICAL|MODERATE|SAFE>",
          "reason": "<Specific biological mechanism why this product impacts their pathology or goal>"
        }
      ],
      "dynamicAlternatives": [
        {
          "name": "<Real cleaner Indian alternative brand and product matching their goal>",
          "scoreOutOf100": 85,
          "whyBetterThanScanned": "<Direct comparative nutritional advantage aligned with their fitness target>",
          "cleanSearchQuery": "<Precise search query for Blinkit/Zepto/Instamart>",
          "alternativeSummary": "<e.g., 18g Protein • Zero Palm Oil • High Fiber>",
          "reason": "<Reason for recommendation>"
        }
      ]
    }
""".trimIndent()

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
                        put("temperature", 0.1)
                    })
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$key")
                    .addHeader("x-goog-api-key", key)
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
        userConditions: List<String>,
        userProfile: UserProfile? = null
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

        val conditionsText = userConditions.joinToString(", ").ifBlank { "None declared" }
        val fitnessGoal = userProfile?.fitnessGoal?.ifBlank { "Maintain & Clean Eating" } ?: "Maintain & Clean Eating"
        val userWeightKg = userProfile?.weightKg?.let { "$it kg" } ?: "Not provided"

        val prompt = """
            You are an authoritative clinical food forensic auditor and sports nutritionist specializing in FSSAI and packaged foods.
            
            OBJECTIVE:
            Evaluate the scanned product against the user's specific health directives and biometrics.
            
            USER CLINICAL PROFILE:
            - Declared Conditions & Directives: [$conditionsText]
            - Target Fitness Goal: $fitnessGoal
            - Body Weight: $userWeightKg

            CLINICAL AUDIT DIRECTIVE:
            1. If the user lists custom, serious conditions (such as Cancer, GBS, Celiac, or Renal Impairment), you MUST search ingredients for carcinogens, inflammatory seed oils, artificial sweeteners (e.g., Aspartame, Sucralose), high sodium, or specific contraindications related to those conditions.
            2. Explicitly explain the impact in `aiExplanation` and output corresponding directives in `personalizedWarnings`.
            
            SCANNED OCR TEXT:
            \"\"\"
            $scannedText
            \"\"\"
            Product Hint: ${detectedBrandHint ?: "Indian packaged food"}

            Respond strictly in valid JSON matching the schema with fields: productName, score, verdict, summary, aiExplanation, scoreAuditReason, scannedMacroSummary, nutritionTable, scoreFactors, fullIngredientsList, simplifiedIngredients, healthBenefits, warnings, containsPalmOil, palmOilDetails, personalizedWarnings, dynamicAlternatives.
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("model", model)
            put("temperature", 0.1)
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