package com.example.nutriwise.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriwise.data.AiExplanationService
import com.example.nutriwise.data.DualScannerEngine
import com.example.nutriwise.data.DynamicProductImageService
import com.example.nutriwise.data.FirebaseRepository
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


    private val aiService = AiExplanationService(apiKey = "GEMINI_API_KEY (3.6)")

    private val _uiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    fun processCapturedImage(bitmap: Bitmap) {
        _uiState.value = ScanUiState.Processing

        viewModelScope.launch {
            try {
                // 1. Fetch user profile & dietary conditions
                val userProfile = firebaseRepo.fetchCurrentUserProfile() ?: UserProfile()
                val userConditions = userProfile.healthConditions

                // 2. OCR & Barcode extraction
                val detection = dualScanner.analyzeImage(bitmap)

                if (detection.ingredientsText.isBlank() && detection.verifiedProductName == null) {
                    _uiState.value = ScanUiState.Error("No readable barcode or ingredient label found. Please hold the camera steady.")
                    return@launch
                }

                // 3. Dynamic Forensic Extraction via xAI Grok
                val hint = detection.verifiedProductName?.let { "$it by ${detection.verifiedBrand ?: ""}" }
                val dynamicResult = aiService.analyzeLabelDynamically(
                    scannedText = detection.ingredientsText,
                    detectedBrandHint = hint,
                    userConditions = userConditions
                )

                // 4. Fetch cleaner alternative images concurrently
                val enrichedAlternatives = dynamicResult.dynamicAlternatives.map { alt ->
                    async {
                        val fetchedImg = imageService.fetchRealProductImage(alt.cleanSearchQuery)
                        alt.copy(imageUrl = fetchedImg)
                    }
                }.awaitAll()

                val resolvedProductName = detection.verifiedProductName ?: dynamicResult.productName

                // 5. Package full analysis report
                val fullAnalysis = FullProductAnalysis(
                    productName = resolvedProductName,
                    brandName = detection.verifiedBrand,
                    productImageUrl = detection.verifiedImageUrl,
                    scoreOutOf100 = dynamicResult.score,
                    ratingVerdict = dynamicResult.verdict,
                    summaryInSimpleLanguage = dynamicResult.summary,
                    aiExplanation = dynamicResult.aiExplanation,
                    scoreAuditExplanation = dynamicResult.scoreAuditReason,
                    scoreFactors = dynamicResult.scoreFactors,
                    nutritionTable = dynamicResult.nutritionTable,
                    fullIngredientsList = dynamicResult.fullIngredientsList,
                    simplifiedIngredients = dynamicResult.simplifiedIngredients,
                    healthBenefits = dynamicResult.healthBenefits,
                    containsPalmOil = dynamicResult.containsPalmOil,
                    palmOilDetails = dynamicResult.palmOilDetails,
                    suggestedAlternatives = enrichedAlternatives,
                    personalizedWarnings = dynamicResult.personalizedWarnings
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