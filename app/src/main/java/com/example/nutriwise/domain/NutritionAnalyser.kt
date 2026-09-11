package com.example.nutriwise.domain

import com.example.nutriwise.data.ProductDto
import kotlin.math.roundToInt

class NutritionAnalyser {

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

    fun analyze(product: ProductDto, userProfile: HealthProfile): ProductAssessment {
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

        // 6. Allergen Checks
        val allergensInProduct = (product.allergensTags ?: emptyList()).map { it.removePrefix("en:").lowercase() }
        for (avoidAllergen in userProfile.allergenAvoidList) {
            val normalizedAvoid = avoidAllergen.lowercase().trim()
            if (allergensInProduct.any { it.contains(normalizedAvoid) } || ingredientsText.contains(normalizedAvoid)) {
                allergenWarnings.add("Warning: Matches allergen preference ($avoidAllergen).")
            }
        }

        // 7. Personalization Notes
        if (userProfile.hasDiabetes && sugarLevel == Level.HIGH) {
            personalizedNotes.add("High sugar content (${sugars}g/100g). May lead to rapid glycemic spike.")
        }
        if (userProfile.hasHypertension && sodiumLevel == Level.HIGH) {
            personalizedNotes.add("High sodium content (${(sodium * 1000).roundToInt()}mg/100g). Monitor daily intake.")
        }
        if (userProfile.isHighProteinGoal && protein >= 10.0) {
            personalizedNotes.add("High protein content (${protein}g/100g) aligns with your daily protein goal.")
        }

        // 8. 100-Point Algorithmic Score
        var score = 90.0
        if (sugarLevel == Level.HIGH) score -= 22.0 else if (sugarLevel == Level.MODERATE) score -= 10.0
        if (sodiumLevel == Level.HIGH) score -= 18.0 else if (sodiumLevel == Level.MODERATE) score -= 8.0
        if (satFatLevel == Level.HIGH) score -= 20.0 else if (satFatLevel == Level.MODERATE) score -= 8.0
        if (fiber >= 3.0) score += 6.0
        if (protein >= 8.0) score += 6.0
        if (containsPalmOil) score -= 10.0

        val finalScore = score.coerceIn(15.0, 95.0)

        return ProductAssessment(
            overallScore = (finalScore * 10.0).roundToInt() / 10.0,
            nutrientInsights = insights,
            containsPalmOil = containsPalmOil,
            palmOilDetails = palmOilDetails,
            allergenWarnings = allergenWarnings,
            personalizedNotes = personalizedNotes
        )
    }
}