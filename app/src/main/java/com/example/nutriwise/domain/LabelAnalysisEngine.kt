package com.example.nutriwise.domain

import com.example.nutriwise.data.InsAdditiveDatabase

class LabelAnalysisEngine {

    // Common synonyms/derivatives of palm oil on food labels
    private val palmOilKeywords = listOf(
        "palm oil", "palmolein", "palm kernel oil",
        "fractionated palm oil", "hydrogenated palm oil", "elaeis guineensis",
        "palmitic acid", "sodium palmitate"
    )

    // Allergen dictionary mapped to common ingredient synonyms
    private val commonAllergens = mapOf(
        "Gluten" to listOf("wheat", "gluten", "barley", "rye", "maida", "semolina", "atta"),
        "Dairy / Lactose" to listOf("milk", "whey", "casein", "lactose", "butter", "cheese", "milk solids", "curd"),
        "Peanuts" to listOf("peanut", "groundnut"),
        "Soy" to listOf("soy", "soya", "soy lecithin", "soy protein"),
        "Tree Nuts" to listOf("almond", "cashew", "walnut", "pistachio", "hazelnut"),
        "Egg" to listOf("egg", "albumin", "egg yolk", "egg white")
    )

    fun analyzeExtractedText(rawText: String): FullProductAnalysis {
        val lowerText = rawText.lowercase()

        // 1. Detect Category Dynamically from predefined catalog
        val category = CategoryDatabase.detectCategory(lowerText)
        val categoryDisplayName = category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }

        // 2. Health Condition Checks & Warnings
        val conditionWarnings = mutableListOf<ConditionWarning>()
        val healthBenefits = mutableListOf<String>()

        // A. Diabetes / Refined Sugar Check
        val hasSugar = lowerText.contains("sugar") || lowerText.contains("glucose") ||
                lowerText.contains("corn syrup") || lowerText.contains("invert syrup") ||
                lowerText.contains("fructose") || lowerText.contains("maltodextrin")

        if (hasSugar) {
            conditionWarnings.add(
                ConditionWarning(
                    condition = "Nutrient Note: Refined Sugars / High-Glycemic Syrups",
                    message = "Added sugars or high-glycemic syrups detected in ingredient declaration. Consider moderating intake if monitoring blood glucose."
                )
            )
        }

        // B. Blood Pressure / Sodium Check
        val hasSodium = lowerText.contains("sodium") || lowerText.contains("salt") ||
                lowerText.contains("msg") || lowerText.contains("monosodium glutamate") ||
                lowerText.contains("baking soda")

        if (hasSodium) {
            conditionWarnings.add(
                ConditionWarning(
                    condition = "Nutrient Note: Added Sodium / Salt Compounds",
                    message = "Sodium compounds detected in ingredients. Monitor overall daily intake if following a sodium-restricted diet."
                )
            )
        }

        // C. Cholesterol & Fat Saturation Check
        val containsPalmOil = palmOilKeywords.any { lowerText.contains(it) }
        val hasBadFats = lowerText.contains("hydrogenated") || lowerText.contains("trans fat") ||
                lowerText.contains("partially hydrogenated") || containsPalmOil

        if (hasBadFats) {
            conditionWarnings.add(
                ConditionWarning(
                    condition = "Nutrient Note: Saturated / Hydrogenated Fats",
                    message = "Contains refined saturated vegetable oils (Palmolein / Palm fractions). Consider balancing with unsaturated healthy fat sources."
                )
            )
        }

        // 3. Positive Whole-Food & Micronutrient Benefits
        if (lowerText.contains("whole wheat") || lowerText.contains("oats") || lowerText.contains("fiber") || lowerText.contains("millet") || lowerText.contains("ragi")) {
            healthBenefits.add("Contains whole grains or dietary fiber for digestive balance.")
        }
        if (lowerText.contains("protein") || lowerText.contains("almond") || lowerText.contains("whey") || lowerText.contains("pea protein") || lowerText.contains("lentil")) {
            healthBenefits.add("Contains plant/dairy protein to support satiety.")
        }
        if (lowerText.contains("chia") || lowerText.contains("flax") || lowerText.contains("walnut") || lowerText.contains("olive oil")) {
            healthBenefits.add("Contains healthy unsaturated fatty acids.")
        }
        if (healthBenefits.isEmpty()) {
            healthBenefits.add("Provides quick dietary energy.")
        }

        // 4. Palm Oil Scan Details
        val palmOilDetails = if (containsPalmOil) {
            "Contains Edible Vegetable Oil (Palmolein / Palm fractions), which is rich in saturated fatty acids."
        } else null

        // 5. Additive Detection & Decoding
        val simplifiedIngredients = mutableListOf<String>()
        val foundInsCodes = Regex("(?i)ins\\s*(\\d+[a-z]?(?:\\([ivx]+\\))?)").findAll(rawText)
        for (match in foundInsCodes) {
            val code = match.value
            simplifiedIngredients.add(InsAdditiveDatabase.decodeAdditive(code))
        }

        // 6. Calibrated Algorithmic Score & Auditable Factors
        var score = 90
        if (hasSugar) score -= 18
        if (hasBadFats) score -= 15
        if (hasSodium) score -= 12
        if (containsPalmOil) score -= 8
        if (healthBenefits.size >= 2) score += 12

        val finalScore = score.coerceIn(20, 95)

        val verdict = when {
            finalScore >= 75 -> "Nutritious Choice"
            finalScore >= 50 -> "Moderate / Occasional Choice"
            else -> "Ultra-Processed / Consume Sparingly"
        }

        val scoreFactors = listOf(
            ScoreFactor("Processing Degree", if (hasBadFats) "Ultra-Processed" else "Minimally Processed", !hasBadFats),
            ScoreFactor("Added Sugars", if (hasSugar) "Detected (Sugar/Syrups)" else "Low / Not Detected", !hasSugar),
            ScoreFactor("Saturated Fat", if (containsPalmOil) "Palm Oil / High Saturated Fat" else "Low to Moderate", !containsPalmOil),
            ScoreFactor("Sodium Load", if (hasSodium) "Added Salt/Sodium Present" else "Low", !hasSodium),
            ScoreFactor("Additive Load", if (simplifiedIngredients.isNotEmpty()) "${simplifiedIngredients.size} Additives Detected" else "Minimal", simplifiedIngredients.isEmpty())
        )

        val scoreAuditExplanation = buildString {
            if (hasSugar && containsPalmOil) {
                append("Refined sweeteners and palmolein reduced the score, ")
            } else if (hasSugar) {
                append("Added sugars reduced the score, ")
            } else if (containsPalmOil) {
                append("Palmolein content reduced the score, ")
            } else {
                append("Whole-food components supported the score, ")
            }
            if (hasSodium) {
                append("along with added sodium compounds.")
            } else {
                append("while low sodium levels helped balance it.")
            }
        }

        // 7. Dynamic Alternatives Based on Category
        val alternatives = CategoryDatabase.getSmartAlternatives(
            category = category,
            hasDiabetes = hasSugar,
            hasHighBp = hasSodium
        )

        // 8. Extract raw ingredient lines
        val fullIngredientsList = rawText.lines()
            .map { it.trim().removePrefix("-").removePrefix("•").trim() }
            .filter { it.isNotBlank() && it.length > 2 }
            .take(15)

        return FullProductAnalysis(
            productName = "Identified Category: $categoryDisplayName",
            brandName = null,
            productImageUrl = null,
            scoreOutOf100 = finalScore,
            ratingVerdict = verdict,
            summaryInSimpleLanguage = "Product evaluated under $categoryDisplayName. Overall Health Score: $finalScore/100 ($verdict).",
            aiExplanation = null,
            scoreAuditExplanation = scoreAuditExplanation,
            scoreFactors = scoreFactors,
            nutritionTable = emptyList(),
            fullIngredientsList = fullIngredientsList,
            simplifiedIngredients = simplifiedIngredients,
            healthBenefits = healthBenefits,
            conditionWarnings = conditionWarnings,
            containsPalmOil = containsPalmOil,
            palmOilDetails = palmOilDetails,
            suggestedAlternatives = alternatives
        )
    }
}