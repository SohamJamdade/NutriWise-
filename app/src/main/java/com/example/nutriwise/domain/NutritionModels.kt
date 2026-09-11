package com.example.nutriwise.domain

enum class Level {
    LOW,
    MODERATE,
    HIGH
}

data class NutrientInsight(
    val name: String,
    val amountPer100g: Double,
    val unit: String,
    val level: Level,
    val summary: String
)

data class NutritionFact(
    val nutrientName: String,
    val amountPer100g: String,
    val status: String? = null
)

data class ScoreFactor(
    val factorName: String,
    val status: String,
    val isFavorable: Boolean
)

data class MacroComparison(
    val scannedSummary: String,
    val alternativeSummary: String
)

data class HealthAlternative(
    val name: String,
    val estimatedPrice: String? = null,
    val scoreOutOf100: Int = 85,
    val whyBetterThanScanned: String = "Made with whole grains and zero industrial palm oil.",
    val cleanSearchQuery: String = name,
    val imageUrl: String? = null,
    val availableOn: List<String> = listOf("Blinkit", "Zepto", "Instamart"),
    val macroComparison: MacroComparison? = null,
    val reason: String = ""
)

data class ConditionWarning(
    val condition: String,
    val severity: String = "Moderate",
    val message: String
)

data class HealthProfile(
    val hasDiabetes: Boolean = false,
    val hasHypertension: Boolean = false,
    val hasHighCholesterol: Boolean = false,
    val isHighProteinGoal: Boolean = false,
    val allergenAvoidList: List<String> = emptyList()
)

data class ProductAssessment(
    val overallScore: Double,
    val nutrientInsights: List<NutrientInsight>,
    val containsPalmOil: Boolean,
    val palmOilDetails: String?,
    val allergenWarnings: List<String>,
    val personalizedNotes: List<String>
)

data class FullProductAnalysis(
    val productName: String,
    val brandName: String? = null,
    val productImageUrl: String? = null,
    val scoreOutOf100: Int,
    val ratingVerdict: String,
    val summaryInSimpleLanguage: String,
    val aiExplanation: String? = null,
    val scoreAuditExplanation: String = "",
    val scoreFactors: List<ScoreFactor> = emptyList(),
    val nutritionTable: List<NutritionFact> = emptyList(),
    val fullIngredientsList: List<String> = emptyList(),
    val simplifiedIngredients: List<String> = emptyList(),
    val healthBenefits: List<String> = emptyList(),
    val conditionWarnings: List<ConditionWarning> = emptyList(),
    val nutrientInsights: List<NutrientInsight> = emptyList(),
    val containsPalmOil: Boolean = false,
    val palmOilDetails: String? = null,
    val allergenWarnings: List<String> = emptyList(),
    val suggestedAlternatives: List<HealthAlternative> = emptyList()
)