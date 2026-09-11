package com.example.nutriwise.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriwise.data.AiExplanationService
import com.example.nutriwise.data.DualScannerEngine
import com.example.nutriwise.data.DynamicProductImageService
import com.example.nutriwise.data.FirebaseRepository
import com.example.nutriwise.domain.ConditionWarning
import com.example.nutriwise.domain.FullProductAnalysis
import com.example.nutriwise.domain.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Processing : ScanUiState
    data class Success(
        val analysis: FullProductAnalysis,
        val capturedBitmap: Bitmap?
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScannerViewModel : ViewModel() {

    private val dualScanner = DualScannerEngine()
    private val imageService = DynamicProductImageService()
    private val firebaseRepo = FirebaseRepository()

    // Consider moving this API key to BuildConfig / local.properties for security
    private val aiService = AiExplanationService(apiKey = "YOUR_GROQ_API_KEY")

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun processCapturedImage(bitmap: Bitmap) {
        _uiState.value = ScanUiState.Processing

        viewModelScope.launch {
            try {
                // 1. Fetch user clinical profile
                val userProfile = firebaseRepo.fetchCurrentUserProfile() ?: UserProfile()

                // 2. Concurrent OCR & Barcode extraction
                val detection = dualScanner.analyzeImage(bitmap)

                if (detection.ingredientsText.isBlank() && detection.verifiedProductName == null) {
                    _uiState.value = ScanUiState.Error("No readable barcode or ingredient label found. Please hold the camera steady.")
                    return@launch
                }

                // 3. AI Dynamic Analysis
                val hint = detection.verifiedProductName?.let { "$it by ${detection.verifiedBrand ?: ""}" }
                val dynamicResult = aiService.analyzeLabelDynamically(detection.ingredientsText, hint)

                // 4. Fetch clean packshot images concurrently for dynamic swaps
                val enrichedAlternatives = dynamicResult.dynamicAlternatives.map { alt ->
                    async {
                        val fetchedImg = imageService.fetchRealProductImage(alt.cleanSearchQuery)
                        alt.copy(imageUrl = fetchedImg)
                    }
                }.awaitAll()

                // 5. Generate Personalized Medical Alerts
                val personalizedWarnings = mutableListOf<ConditionWarning>()

                // Profile Condition 1: Diabetes
                if (userProfile.hasDiabetes) {
                    val sugarFact = dynamicResult.nutritionTable.find {
                        it.nutrientName.contains("Sugar", ignoreCase = true)
                    }
                    val isHighSugar = sugarFact?.status?.equals("High", ignoreCase = true) == true ||
                            detection.ingredientsText.contains("sugar", ignoreCase = true) ||
                            detection.ingredientsText.contains("invert syrup", ignoreCase = true)

                    if (isHighSugar) {
                        personalizedWarnings.add(
                            ConditionWarning(
                                condition = "🚨 Diabetes Profile Alert",
                                message = "High glycemic / added sugar load detected. May trigger a rapid blood glucose spike."
                            )
                        )
                    }
                }

                // Profile Condition 2: Hypertension (High BP)
                if (userProfile.hasHypertension) {
                    val sodiumFact = dynamicResult.nutritionTable.find {
                        it.nutrientName.contains("Sodium", ignoreCase = true)
                    }
                    val isHighSodium = sodiumFact?.status?.equals("High", ignoreCase = true) == true ||
                            detection.ingredientsText.contains("salt", ignoreCase = true) ||
                            detection.ingredientsText.contains("sodium", ignoreCase = true)

                    if (isHighSodium) {
                        personalizedWarnings.add(
                            ConditionWarning(
                                condition = "🚨 Hypertension Alert",
                                message = "Elevated sodium detected. Exceeds recommended single-serving threshold for blood pressure control."
                            )
                        )
                    }
                }

                // Profile Condition 3: High Cholesterol / Palm Oil
                if (userProfile.hasHighCholesterol && dynamicResult.containsPalmOil) {
                    personalizedWarnings.add(
                        ConditionWarning(
                            condition = "🚨 Heart Health Alert",
                            message = "Contains industrial palm/palmolein fractions rich in saturated palmitic acid."
                        )
                    )
                }

                // Profile Condition 4: User Allergen Flags
                for (allergen in userProfile.allergenAvoidList) {
                    if (detection.ingredientsText.contains(allergen, ignoreCase = true)) {
                        personalizedWarnings.add(
                            ConditionWarning(
                                condition = "⚠️ Allergen Detected ($allergen)",
                                message = "Matches your flagged allergen blacklist."
                            )
                        )
                    }
                }

                // Append general AI warnings
                dynamicResult.warnings.forEach { (condition, message) ->
                    personalizedWarnings.add(ConditionWarning(condition = condition, message = message))
                }

                val resolvedProductName = detection.verifiedProductName ?: dynamicResult.productName

                val fullAnalysis = FullProductAnalysis(
                    productName = resolvedProductName,
                    brandName = detection.verifiedBrand,
                    productImageUrl = detection.verifiedImageUrl,
                    scoreOutOf100 = dynamicResult.score,
                    ratingVerdict = dynamicResult.verdict,
                    summaryInSimpleLanguage = dynamicResult.summary,
                    scoreAuditExplanation = dynamicResult.scoreAuditReason,
                    scoreFactors = dynamicResult.scoreFactors,
                    nutritionTable = dynamicResult.nutritionTable,
                    fullIngredientsList = dynamicResult.fullIngredientsList,
                    simplifiedIngredients = dynamicResult.simplifiedIngredients,
                    healthBenefits = dynamicResult.healthBenefits,
                    conditionWarnings = personalizedWarnings,
                    containsPalmOil = dynamicResult.containsPalmOil,
                    palmOilDetails = dynamicResult.palmOilDetails,
                    suggestedAlternatives = enrichedAlternatives
                )

                _uiState.value = ScanUiState.Success(
                    analysis = fullAnalysis,
                    capturedBitmap = bitmap
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = ScanUiState.Error("Analysis failed: ${e.localizedMessage ?: "Please try again."}")
            }
        }
    }

    fun resetScan() {
        _uiState.value = ScanUiState.Idle
    }
}