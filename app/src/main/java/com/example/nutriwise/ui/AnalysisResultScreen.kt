package com.example.nutriwise.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nutriwise.data.FirebaseRepository
import com.example.nutriwise.domain.FullProductAnalysis
import com.example.nutriwise.domain.HealthAlternative
import com.example.nutriwise.domain.NutritionFact
import com.example.nutriwise.util.QuickCommerceLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    analysis: FullProductAnalysis,
    capturedBitmap: Bitmap? = null,
    onScanAgain: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val firebaseRepo = remember { FirebaseRepository() }

    var showShareDialog by remember { mutableStateOf(false) }
    var userCustomOpinion by remember { mutableStateOf(analysis.summaryInSimpleLanguage) }
    var isPosting by remember { mutableStateOf(false) }
    var postSuccess by remember { mutableStateOf(false) }

    val primaryThemeColor = when {
        analysis.scoreOutOf100 >= 70 -> Color(0xFF1B5E20) // Deep Emerald
        analysis.scoreOutOf100 >= 45 -> Color(0xFFE65100) // Deep Amber/Orange
        else -> Color(0xFFC62828)                         // Crimson
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Nutritional Forensic Report",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onScanAgain) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }, enabled = !postSuccess) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = if (postSuccess) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // 1. HERO ANIMATED CIRCULAR SCORE CARD
            // -------------------------------------------------------------
            HealthScoreHero(
                score = analysis.scoreOutOf100,
                productName = analysis.productName,
                brandName = analysis.brandName,
                verdict = analysis.ratingVerdict,
                auditReason = analysis.scoreAuditExplanation,
                productImageUrl = analysis.productImageUrl,
                capturedBitmap = capturedBitmap
            )

            // -------------------------------------------------------------
            // 2. PERSONALIZED AI HEALTH DIRECTIVES
            // -------------------------------------------------------------
            val hasCriticalWarning = analysis.personalizedWarnings.any { it.severity.equals("CRITICAL", ignoreCase = true) }
            val hasModerateWarning = analysis.personalizedWarnings.any { it.severity.equals("MODERATE", ignoreCase = true) }

            val directiveBg = when {
                hasCriticalWarning -> Color(0xFF2A1215)
                hasModerateWarning -> Color(0xFF2E1F10)
                else -> Color(0xFF0E2417)
            }
            val directiveBorder = when {
                hasCriticalWarning -> Color(0xFFEF5350)
                hasModerateWarning -> Color(0xFFFFB74D)
                else -> Color(0xFF66BB6A)
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = directiveBg),
                border = BorderStroke(1.2.dp, directiveBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = directiveBorder.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = when {
                                        hasCriticalWarning -> "🚫"
                                        hasModerateWarning -> "⚠️"
                                        else -> "🛡️"
                                    },
                                    fontSize = 15.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when {
                                hasCriticalWarning -> "Critical Profile Alert"
                                hasModerateWarning -> "Moderate Consumption Warning"
                                else -> "Compatible with Profile"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (analysis.personalizedWarnings.isNotEmpty()) {
                            analysis.personalizedWarnings.joinToString(separator = "\n\n") { warning ->
                                "• ${warning.condition}: ${warning.reason}"
                            }
                        } else if (!analysis.aiExplanation.isNullOrBlank()) {
                            analysis.aiExplanation
                        } else {
                            "Based on your clinical conditions, this product contains no immediate contraindications."
                        },
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFECEFF1)
                    )
                }
            }

            // -------------------------------------------------------------
            // 3. CLINICAL SUMMARY
            // -------------------------------------------------------------
            if (analysis.summaryInSimpleLanguage.isNotBlank()) {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = primaryThemeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = analysis.summaryInSimpleLanguage,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = Color(0xFF37474F)
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. VISUAL NUTRITION FACTS & BAR GAUGES
            // -------------------------------------------------------------
            if (analysis.nutritionTable.isNotEmpty()) {
                Text(
                    text = "Nutrition Breakdown (Per 100g)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        analysis.nutritionTable.forEach { fact ->
                            NutrientProgressBarRow(fact)
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. INGREDIENTS DECLARATION
            // -------------------------------------------------------------
            if (analysis.fullIngredientsList.isNotEmpty()) {
                Text(
                    text = "Declared Ingredients",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        analysis.fullIngredientsList.forEachIndexed { index, ingredient ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0288D1),
                                    modifier = Modifier.width(22.dp)
                                )
                                Text(
                                    text = ingredient,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF455A64)
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. DECODED ADDITIVES & E-NUMBERS
            // -------------------------------------------------------------
            if (analysis.simplifiedIngredients.isNotEmpty()) {
                Text(
                    text = "Decoded Additives & INS Numbers (${analysis.simplifiedIngredients.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(18.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        analysis.simplifiedIngredients.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("🧪", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = item,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    color = Color(0xFF263238)
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 7. PALM OIL BANNER
            // -------------------------------------------------------------
            if (analysis.containsPalmOil) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌴", fontSize = 26.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Palm Oil / Palmolein Detected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = analysis.palmOilDetails ?: "Contains fractionated palm fat high in saturated fatty acids.",
                                fontSize = 12.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 8. VERIFIED QUICK-COMMERCE SWAPS
            // -------------------------------------------------------------
            if (analysis.suggestedAlternatives.isNotEmpty()) {
                Text(
                    text = "Healthier Alternatives",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                analysis.suggestedAlternatives.forEach { alt ->
                    AlternativeProductCard(
                        alternative = alt,
                        onBlinkitClick = { query -> QuickCommerceLauncher.openBlinkit(context, query) },
                        onZeptoClick = { query -> QuickCommerceLauncher.openZepto(context, query) },
                        onInstamartClick = { query -> QuickCommerceLauncher.openInstamart(context, query) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // -------------------------------------------------------------
            // 9. BOTTOM ACTION BUTTONS
            // -------------------------------------------------------------
            Button(
                onClick = { showShareDialog = true },
                enabled = !postSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
            ) {
                Text(
                    text = if (postSuccess) "✓ Shared to Community Feed" else "📢 Share Review to Community",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }

            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, Color(0xFFCFD8DC))
            ) {
                Text(
                    "Scan Another Item",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF37474F)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // -------------------------------------------------------------
    // SHARE MODAL DIALOG
    // -------------------------------------------------------------
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPosting) showShareDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Post Review to Feed", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Share this analysis and your personal thoughts with the NutriWise community:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = userCustomOpinion,
                        onValueChange = { userCustomOpinion = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("What did you think of the ingredients, taste, or alternative swap?") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isPosting = true
                        coroutineScope.launch {
                            try {
                                val finalImageUrl = if (!analysis.productImageUrl.isNullOrBlank() && analysis.productImageUrl.startsWith("http")) {
                                    analysis.productImageUrl
                                } else if (capturedBitmap != null) {
                                    firebaseRepo.convertBitmapToBase64(capturedBitmap)
                                } else {
                                    ""
                                }

                                firebaseRepo.createReviewPost(
                                    productName = analysis.productName,
                                    healthScore = analysis.scoreOutOf100,
                                    verdict = analysis.ratingVerdict,
                                    reviewText = userCustomOpinion,
                                    packetImageUrl = finalImageUrl,
                                    suggestedSwap = analysis.suggestedAlternatives.firstOrNull()?.name ?: ""
                                )

                                postSuccess = true
                                showShareDialog = false
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isPosting = false
                            }
                        }
                    },
                    enabled = !isPosting && userCustomOpinion.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Post Review", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showShareDialog = false },
                    enabled = !isPosting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// SUBCOMPONENTS
// -------------------------------------------------------------

@Composable
fun HealthScoreHero(
    score: Int,
    productName: String,
    brandName: String?,
    verdict: String,
    auditReason: String,
    productImageUrl: String?,
    capturedBitmap: Bitmap?
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ScoreGauge"
    )

    val scoreColor = when {
        score >= 70 -> Color(0xFF2E7D32)
        score >= 45 -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }

    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayImage: Any? = when {
                    !productImageUrl.isNullOrBlank() -> productImageUrl
                    capturedBitmap != null -> capturedBitmap
                    else -> null
                }

                if (displayImage != null) {
                    AsyncImage(
                        model = displayImage,
                        contentDescription = productName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF1F5F9))
                            .padding(4.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = productName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                    if (!brandName.isNullOrBlank()) {
                        Text(
                            text = brandName,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Progress Gauge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor.copy(alpha = 0.12f),
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = scoreColor,
                    strokeWidth = 10.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = scoreColor
                    )
                    Text(
                        text = "out of 100",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Verdict Pill
            Surface(
                shape = CircleShape,
                color = scoreColor.copy(alpha = 0.12f)
            ) {
                Text(
                    text = verdict.uppercase(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = scoreColor
                )
            }

            if (auditReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔬 Audit Reason: $auditReason",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Color(0xFF475569),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NutrientProgressBarRow(fact: NutritionFact) {
    val statusText = fact.status.orEmpty()
    val statusLower = statusText.lowercase()

    val statusColor = when (statusLower) {
        "low" -> Color(0xFF2E7D32)
        "moderate" -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }

    val progressValue = when (statusLower) {
        "low" -> 0.25f
        "moderate" -> 0.65f
        else -> 1.0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = fact.nutrientName,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1E293B)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = fact.amountPer100g,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusText.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progressValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = statusColor,
            trackColor = Color(0xFFF1F5F9),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun AlternativeProductCard(
    alternative: HealthAlternative,
    onBlinkitClick: (String) -> Unit,
    onZeptoClick: (String) -> Unit,
    onInstamartClick: (String) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!alternative.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = alternative.imageUrl,
                        contentDescription = alternative.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Fastfood, contentDescription = null, tint = Color(0xFF2E7D32))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alternative.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Instant Quick-Commerce Swap",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF2E7D32)
                ) {
                    Text(
                        text = "${alternative.scoreOutOf100}/100",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0FDF4),
                border = BorderStroke(1.dp, Color(0xFFDCFCE7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏆 Advantage: ${alternative.whyBetterThanScanned}",
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFF166534),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (alternative.macroComparison != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Scanned: ${alternative.macroComparison.scannedSummary}",
                            fontSize = 11.sp,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Swap: ${alternative.macroComparison.alternativeSummary}",
                            fontSize = 11.sp,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onBlinkitClick(alternative.cleanSearchQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7CB45)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Blinkit", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onZeptoClick(alternative.cleanSearchQuery) },
                    border = BorderStroke(1.dp, Color(0xFF7B1FA2)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Zepto", color = Color(0xFF7B1FA2), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onInstamartClick(alternative.cleanSearchQuery) },
                    border = BorderStroke(1.dp, Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Instamart", color = Color(0xFFE65100), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}