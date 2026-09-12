package com.example.nutriwise.domain

import android.util.Log
import com.example.nutriwise.data.ProductDto
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.roundToInt

class NutritionAnalyser(private val geminiApiKey: String? = null) {

    private val palmOilKeywords = listOf(
        "palm oil",
        "palmolein",
        "palm kernel oil",
        "fractionated palm oil",
        "hydrogenated palm oil",
        "elaeis guineensis",
        "palmitate",
        "palmitic acid"
    )

    suspend fun analyze(
        product: ProductDto,
        userConditions: List<String>
    ): Pair<ProductAssessment, List<DynamicHealthWarning>> {
        val nutriments = product.nutriments
        val insights = mutableListOf<NutrientInsight>()
        val personalizedNotes = mutableListOf<String>()
        val allergenWarnings = mutableListOf<String>()

        // 1. Sugar Assessment (WHO standard per 100g)
        val sugars = nutriments?.sugars100g ?: 0.0
        val sugarLevel = when {
            sugars <= 5.0 -> Level.LOW
            sugars <= 22.5 -> Level.MODERATE
            else -> Level.HIGH
        }
        insights.add(
            NutrientInsight(
                name = "Sugar",
                amountPer100g = sugars,
                unit = "g",
                level = sugarLevel,
                summary = when (sugarLevel) {
                    Level.LOW -> "Low in added sugar."
                    Level.MODERATE -> "Moderate amount of sugar."
                    Level.HIGH -> "High in sugar per 100g."
                }
            )
        )

        // 2. Sodium Assessment
        val sodium = nutriments?.sodium100g ?: 0.0
        val sodiumLevel = when {
            sodium <= 0.12 -> Level.LOW
            sodium <= 0.6 -> Level.MODERATE
            else -> Level.HIGH
        }
        insights.add(
            NutrientInsight(
                name = "Sodium",
                amountPer100g = (sodium * 1000.0).roundToInt().toDouble(),
                unit = "mg",
                level = sodiumLevel,
                summary = when (sodiumLevel) {
                    Level.LOW -> "Low in sodium."
                    Level.MODERATE -> "Moderate sodium content."
                    Level.HIGH -> "High sodium level per 100g."
                }
            )
        )

        // 3. Saturated Fat Assessment
        val satFat = nutriments?.saturatedFat100g ?: 0.0
        val satFatLevel = when {
            satFat <= 1.5 -> Level.LOW
            satFat <= 5.0 -> Level.MODERATE
            else -> Level.HIGH
        }
        insights.add(
            NutrientInsight(
                name = "Saturated Fat",
                amountPer100g = satFat,
                unit = "g",
                level = satFatLevel,
                summary = when (satFatLevel) {
                    Level.LOW -> "Low saturated fat."
                    Level.MODERATE -> "Moderate saturated fat."
                    Level.HIGH -> "High saturated fat content."
                }
            )
        )

        // 4. Protein & Fiber Insights
        val protein = nutriments?.proteins100g ?: 0.0
        val fiber = nutriments?.fiber100g ?: 0.0

        insights.add(
            NutrientInsight(
                name = "Protein",
                amountPer100g = protein,
                unit = "g",
                level = if (protein >= 8.0) Level.HIGH else Level.MODERATE,
                summary = if (protein >= 8.0) "Good source of dietary protein." else "Moderate to low protein."
            )
        )

        if (fiber > 0.0) {
            insights.add(
                NutrientInsight(
                    name = "Dietary Fiber",
                    amountPer100g = fiber,
                    unit = "g",
                    level = if (fiber >= 5.0) Level.HIGH else Level.MODERATE,
                    summary = if (fiber >= 5.0) "High in dietary fiber." else "Moderate dietary fiber."
                )
            )
        }

        // 5. Palm Oil Detection
        val ingredientsText = product.ingredientsText?.lowercase() ?: ""
        val containsPalmOil = palmOilKeywords.any { ingredientsText.contains(it) }
        val palmOilDetails = if (containsPalmOil) {
            "Contains Edible Vegetable Oil (Palmolein / Palm fractions), which is rich in saturated fatty acids."
        } else {
            null
        }

        // 6. Dynamic AI Reasoning against User Conditions
        val dynamicWarnings = if (!geminiApiKey.isNullOrBlank() && userConditions.isNotEmpty()) {
            val aiWarnings = thinkWithGemini(product, userConditions)
            if (aiWarnings.isNotEmpty()) aiWarnings else fallbackDynamicAnalysis(product, userConditions, sugarLevel, sodiumLevel, satFatLevel, containsPalmOil)
        } else {
            fallbackDynamicAnalysis(product, userConditions, sugarLevel, sodiumLevel, satFatLevel, containsPalmOil)
        }

        dynamicWarnings.forEach { warning ->
            personalizedNotes.add("${warning.condition}: ${warning.reason}")
            if (warning.severity.equals("CRITICAL", ignoreCase = true)) {
                allergenWarnings.add("Warning for ${warning.condition}: ${warning.reason}")
            }
        }

        // 7. WHO Algorithmic Score
        var score = 90.0
        if (sugarLevel == Level.HIGH) score -= 22.0 else if (sugarLevel == Level.MODERATE) score -= 10.0
        if (sodiumLevel == Level.HIGH) score -= 18.0 else if (sodiumLevel == Level.MODERATE) score -= 8.0
        if (satFatLevel == Level.HIGH) score -= 20.0 else if (satFatLevel == Level.MODERATE) score -= 8.0
        if (fiber >= 3.0) score += 6.0
        if (protein >= 8.0) score += 6.0
        if (containsPalmOil) score -= 10.0

        val finalScore = score.coerceIn(15.0, 95.0)

        val assessment = ProductAssessment(
            overallScore = (finalScore * 10.0).roundToInt() / 10.0,
            nutrientInsights = insights,
            containsPalmOil = containsPalmOil,
            palmOilDetails = palmOilDetails,
            allergenWarnings = allergenWarnings,
            personalizedNotes = personalizedNotes
        )

        return Pair(assessment, dynamicWarnings)
    }

    private suspend fun thinkWithGemini(
        product: ProductDto,
        userConditions: List<String>
    ): List<DynamicHealthWarning> = withContext(Dispatchers.IO) {
        try {
            // Updated to active model with JSON schema enforcement
            val model = GenerativeModel(
                modelName = "gemini-3.6-flash",
                apiKey = geminiApiKey!!,
                generationConfig = generationConfig {
                    responseMimeType = "application/json"
                    temperature = 0.0f
                }
            )

            val prompt = """
                You are NutriWise AI, a clinical nutritionist.
                Analyze this food product against the user's custom health conditions:
                Product: ${product.productName ?: "Unknown Product"}
                Ingredients: ${product.ingredientsText ?: "Not specified"}
                Sugars per 100g: ${product.nutriments?.sugars100g ?: 0.0}g
                Sodium per 100g: ${product.nutriments?.sodium100g ?: 0.0}g
                Saturated Fat per 100g: ${product.nutriments?.saturatedFat100g ?: 0.0}g
                
                User's Custom Conditions & Allergies:
                ${userConditions.joinToString(", ")}
                
                Evaluate each user condition. Return ONLY a valid JSON array of objects without markdown:
                [
                  {
                    "condition": "Name of condition",
                    "severity": "CRITICAL",
                    "reason": "Clear explanation of why this product affects them"
                  }
                ]
            """.trimIndent()

            val response = model.generateContent(prompt)
            val jsonText = (response.text ?: "")
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val warnings = mutableListOf<DynamicHealthWarning>()
            val jsonArray = JSONArray(jsonText)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                warnings.add(
                    DynamicHealthWarning(
                        condition = obj.optString("condition"),
                        severity = obj.optString("severity", "MODERATE"),
                        reason = obj.optString("reason")
                    )
                )
            }
            warnings
        } catch (e: Exception) {
            Log.e("NutriWiseAI", "Gemini analysis error: ${e.message}", e)
            emptyList()
        }
    }

    private fun fallbackDynamicAnalysis(
        product: ProductDto,
        userConditions: List<String>,
        sugarLevel: Level,
        sodiumLevel: Level,
        satFatLevel: Level,
        containsPalmOil: Boolean
    ): List<DynamicHealthWarning> {
        val warnings = mutableListOf<DynamicHealthWarning>()
        val ingredients = product.ingredientsText?.lowercase() ?: ""

        userConditions.forEach { rawCondition ->
            val cond = rawCondition.lowercase()
            when {
                cond.contains("diabet") || cond.contains("sugar") -> {
                    if (sugarLevel == Level.HIGH) {
                        warnings.add(DynamicHealthWarning(rawCondition, "CRITICAL", "High sugar load (${product.nutriments?.sugars100g}g/100g) risks spiking blood glucose."))
                    } else if (sugarLevel == Level.MODERATE) {
                        warnings.add(DynamicHealthWarning(rawCondition, "MODERATE", "Moderate sugar content. Consume in limited portions."))
                    } else {
                        warnings.add(DynamicHealthWarning(rawCondition, "SAFE", "Low sugar content; generally safer for glycemic balance."))
                    }
                }
                cond.contains("hyper") || cond.contains("bp") || cond.contains("blood pressure") -> {
                    if (sodiumLevel == Level.HIGH) {
                        warnings.add(DynamicHealthWarning(rawCondition, "CRITICAL", "High sodium content exceeds recommended levels for blood pressure management."))
                    }
                }
                cond.contains("cholesterol") || cond.contains("heart") -> {
                    if (satFatLevel == Level.HIGH || containsPalmOil) {
                        warnings.add(DynamicHealthWarning(rawCondition, "CRITICAL", "High saturated fat / palm oil content can elevate LDL cholesterol."))
                    }
                }
                else -> {
                    if (ingredients.contains(cond)) {
                        warnings.add(DynamicHealthWarning(rawCondition, "CRITICAL", "Direct match detected in ingredient statement ($rawCondition)."))
                    }
                }
            }
        }
        return warnings
    }
}