package com.example.nutriwise.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nutriwise.data.FirebaseRepository
import com.example.nutriwise.domain.FullProductAnalysis
import com.example.nutriwise.domain.HealthAlternative
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product Health Report", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onScanAgain) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -------------------------------------------------------------
            // 1. HERO SCORE CARD
            // -------------------------------------------------------------
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        analysis.scoreOutOf100 >= 70 -> Color(0xFFE8F5E9)
                        analysis.scoreOutOf100 >= 45 -> Color(0xFFFFF8E1)
                        else -> Color(0xFFFFEBEE)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val displayImage: Any? = when {
                            !analysis.productImageUrl.isNullOrBlank() -> analysis.productImageUrl
                            capturedBitmap != null -> capturedBitmap
                            else -> null
                        }

                        if (displayImage != null) {
                            AsyncImage(
                                model = displayImage,
                                contentDescription = analysis.productName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = analysis.productName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = analysis.ratingVerdict,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when {
                                    analysis.scoreOutOf100 >= 70 -> Color(0xFF2E7D32)
                                    analysis.scoreOutOf100 >= 45 -> Color(0xFFF57F17)
                                    else -> Color(0xFFC62828)
                                }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                analysis.scoreOutOf100 >= 70 -> Color(0xFF2E7D32)
                                analysis.scoreOutOf100 >= 45 -> Color(0xFFF57F17)
                                else -> Color(0xFFC62828)
                            }
                        ) {
                            Text(
                                text = "${analysis.scoreOutOf100}/100",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (analysis.scoreAuditExplanation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔍 Why this score: ${analysis.scoreAuditExplanation}",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFF263238),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    if (analysis.scoreFactors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Score Evaluation Factors:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        analysis.scoreFactors.forEach { factor ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(factor.factorName, fontSize = 12.sp, color = Color(0xFF37474F))
                                Text(
                                    text = factor.status,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (factor.isFavorable) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NutriWise Algorithmic Score • Independent assessment based on ingredient processing, fat saturation, sodium, and additive load.",
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        color = Color.DarkGray
                    )
                }
            }
            if (analysis.personalizedWarnings.isNotEmpty()) {
                Text(
                    text = "Personal Health Directives",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                analysis.personalizedWarnings.forEach { warning ->
                    val cardColor = when (warning.severity.uppercase()) {
                        "CRITICAL" -> Color(0xFFFFEBEE)
                        "MODERATE" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFE8F5E9)
                    }
                    val iconColor = when (warning.severity.uppercase()) {
                        "CRITICAL" -> Color(0xFFC62828)
                        "MODERATE" -> Color(0xFFE65100)
                        else -> Color(0xFF2E7D32)
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = when (warning.severity.uppercase()) {
                                    "CRITICAL" -> "🚫"
                                    "MODERATE" -> "⚠️"
                                    else -> "✅"
                                },
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${warning.condition} Directive",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = iconColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = warning.reason,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF263238)
                                )
                            }
                        }
                    }
                }
            }

            // Inside ui/AnalysisResultScreen.kt (under the Product Health Score Hero Card):

// -------------------------------------------------------------
// DEDICATED AI HEALTH VERDICT FOR YOUR PROFILE
// -------------------------------------------------------------
            val hasCriticalWarning = analysis.personalizedWarnings.any { it.severity.equals("CRITICAL", ignoreCase = true) }
            val hasModerateWarning = analysis.personalizedWarnings.any { it.severity.equals("MODERATE", ignoreCase = true) }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        hasCriticalWarning -> Color(0xFF2D1517)
                        hasModerateWarning -> Color(0xFF2B1F13)
                        else -> Color(0xFF14241B)
                    }
                ),
                border = BorderStroke(
                    1.5.dp,
                    when {
                        hasCriticalWarning -> Color(0xFFEF5350)
                        hasModerateWarning -> Color(0xFFFFB74D)
                        else -> Color(0xFF81C784)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                hasCriticalWarning -> "🚫 AI Advice: Not Recommended for You"
                                hasModerateWarning -> "⚠️ AI Advice: Consume with Caution"
                                else -> "✅ AI Advice: Safe for Your Health Profile"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = when {
                                hasCriticalWarning -> Color(0xFFFF8A80)
                                hasModerateWarning -> Color(0xFFFFD54F)
                                else -> Color(0xFFA5D6A7)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // AI explanation
                    Text(
                        text = if (analysis.personalizedWarnings.isNotEmpty()) {
                            analysis.personalizedWarnings.joinToString(separator = "\n\n") { warning ->
                                "• ${warning.condition}: ${warning.reason}"
                            }
                        } else {
                            "Based on your health directives, this product does not contain ingredients that conflict with your profile."
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFECEFF1)
                    )
                }
            }

            // -------------------------------------------------------------
            // 2. NUTRITIONAL ASSESSMENT SUMMARY
            // -------------------------------------------------------------
            if (analysis.summaryInSimpleLanguage.isNotBlank()) {
                Text(
                    text = "Nutritional Assessment",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = analysis.summaryInSimpleLanguage,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. NUTRITION FACTS TABLE
            // -------------------------------------------------------------
            if (analysis.nutritionTable.isNotEmpty()) {
                Text("Nutrition Facts (Per 100g)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    border = BorderStroke(1.dp, Color(0xFFDCEDC8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        analysis.nutritionTable.forEach { fact ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(fact.nutrientName, fontSize = 13.sp, color = Color(0xFF33691E), fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(fact.amountPer100g, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                    if (!fact.status.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = when (fact.status.lowercase()) {
                                                "low" -> Color(0xFFE8F5E9)
                                                "moderate" -> Color(0xFFFFF3E0)
                                                else -> Color(0xFFFFEBEE)
                                            }
                                        ) {
                                            Text(
                                                text = fact.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (fact.status.lowercase()) {
                                                    "low" -> Color(0xFF2E7D32)
                                                    "moderate" -> Color(0xFFE65100)
                                                    else -> Color(0xFFC62828)
                                                },
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. INGREDIENTS DECLARATION
            // -------------------------------------------------------------
            if (analysis.fullIngredientsList.isNotEmpty()) {
                Text("Ingredients Declaration", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        analysis.fullIngredientsList.forEachIndexed { index, ingredient ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("${index + 1}. ", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(text = ingredient, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. DECODED ADDITIVES & E-NUMBERS
            // -------------------------------------------------------------
            if (analysis.simplifiedIngredients.isNotEmpty()) {
                Text("Decoded Additives & E-Numbers (${analysis.simplifiedIngredients.size} Detected)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECEFF1)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        analysis.simplifiedIngredients.forEach { item ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text("🧪 ", fontSize = 13.sp)
                                Text(text = item, fontSize = 13.sp, lineHeight = 18.sp, color = Color(0xFF263238))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 6. PALM OIL BANNER
            // -------------------------------------------------------------
            if (analysis.containsPalmOil) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🌴", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = analysis.palmOilDetails ?: "Contains Edible Vegetable Oil (Palmolein / Palm fractions).",
                            fontSize = 13.sp,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 7. DIETARY & HEALTH WARNINGS
            // -------------------------------------------------------------
            if (analysis.conditionWarnings.isNotEmpty()) {
                Text(
                    text = "Dietary & Health Warnings",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                analysis.conditionWarnings.forEach { warning ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = warning.condition, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Black)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = warning.message, fontSize = 12.sp, color = Color(0xFF37474F))
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 8. VERIFIED QUICK-COMMERCE SWAPS
            // -------------------------------------------------------------
            if (analysis.suggestedAlternatives.isNotEmpty()) {
                Text("Healthier Swaps (Quick-Commerce)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                analysis.suggestedAlternatives.forEach { alt ->
                    AlternativeProductCard(
                        alternative = alt,
                        onBlinkitClick = { query -> QuickCommerceLauncher.openBlinkit(context, query) },
                        onZeptoClick = { query -> QuickCommerceLauncher.openZepto(context, query) },
                        onInstamartClick = { query -> QuickCommerceLauncher.openInstamart(context, query) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // -------------------------------------------------------------
            // 9. SHARE TO COMMUNITY BUTTON
            // -------------------------------------------------------------
            Button(
                onClick = { showShareDialog = true },
                enabled = !postSuccess,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (postSuccess) "✓ Posted to Feed" else "📢 Customize & Share to Community",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            // -------------------------------------------------------------
            // 10. SCAN AGAIN BUTTON
            // -------------------------------------------------------------
            OutlinedButton(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Scan Another Product", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // -------------------------------------------------------------
    // INTERACTIVE PRE-POST DIALOG
    // -------------------------------------------------------------
    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { if (!isPosting) showShareDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text("Share Review", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Your post includes the product image and score. Add your review or verdict below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = userCustomOpinion,
                        onValueChange = { userCustomOpinion = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp),
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("What did you think of the taste, ingredients, or alternative?") }
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
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Post Review")
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

@Composable
fun AlternativeProductCard(
    alternative: HealthAlternative,
    onBlinkitClick: (String) -> Unit,
    onZeptoClick: (String) -> Unit,
    onInstamartClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FBE7)),
        border = BorderStroke(1.dp, Color(0xFFDCEDC8)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
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
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Available on Blinkit, Zepto, Instamart",
                        fontSize = 11.sp,
                        color = Color.Gray
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
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFE8F5E9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🏆 ${alternative.whyBetterThanScanned}",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(8.dp)
                )
            }

            if (alternative.macroComparison != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFFFEBEE),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Scanned: ${alternative.macroComparison.scannedSummary}",
                            fontSize = 10.sp,
                            color = Color(0xFFC62828),
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE8F5E9),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Swap: ${alternative.macroComparison.alternativeSummary}",
                            fontSize = 10.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onBlinkitClick(alternative.cleanSearchQuery) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7CB45)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Blinkit", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onZeptoClick(alternative.cleanSearchQuery) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF7B1FA2)),
                    border = BorderStroke(1.dp, Color(0xFF7B1FA2)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Zepto", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onInstamartClick(alternative.cleanSearchQuery) },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
                    border = BorderStroke(1.dp, Color(0xFFE65100)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("Instamart", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}