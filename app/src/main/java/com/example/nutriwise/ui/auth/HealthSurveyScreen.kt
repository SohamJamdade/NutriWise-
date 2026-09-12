package com.example.nutriwise.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriwise.domain.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthOnboardingScreen(
    currentProfile: UserProfile,
    onCompleteOnboarding: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var ageText by remember { mutableStateOf(currentProfile.age?.toString() ?: "") }
    var heightText by remember { mutableStateOf(currentProfile.heightCm?.toString() ?: "") }
    var weightText by remember { mutableStateOf(currentProfile.weightKg?.toString() ?: "") }

    val goals = listOf(
        "Weight Loss / Fat Cut",
        "Weight Gain / Muscle Building",
        "Maintain & Clean Eating",
        "Athletic Performance"
    )
    var selectedGoal by remember { mutableStateOf(currentProfile.fitnessGoal.ifBlank { goals[2] }) }
    var selectedConditions by remember { mutableStateOf(currentProfile.healthConditions.toSet()) }

    val conditionOptions = listOf(
        "Type 2 Diabetes", "Hypertension (High BP)", "High Cholesterol",
        "Fatty Liver", "PCOS / PCOD", "GERD / Acid Reflux", "GBS"
    )

    var isSubmitting by remember { mutableStateOf(false) }

    val cardBg = MaterialTheme.colorScheme.surface
    val inputTextColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Personalize Health & Goals",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "One-time setup. NutriWise AI uses these metrics to balance caloric, macro, and sodium thresholds against your body.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )

            // ---------------------------------------------------------
            // 1. BIOMETRICS SECTION (Age, Height, Weight)
            // ---------------------------------------------------------
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Body Metrics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ageText,
                            onValueChange = { if (it.length <= 3) ageText = it },
                            label = { Text("Age") },
                            textStyle = TextStyle(
                                color = inputTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            label = { Text("Height (cm)") },
                            textStyle = TextStyle(
                                color = inputTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            label = { Text("Weight (kg)") },
                            textStyle = TextStyle(
                                color = inputTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            }

            // ---------------------------------------------------------
            // 2. FITNESS & WEIGHT GOAL
            // ---------------------------------------------------------
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Target Goal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    goals.forEach { goal ->
                        val isSelected = selectedGoal == goal
                        val selectedBg = MaterialTheme.colorScheme.primary
                        val unselectedBg = MaterialTheme.colorScheme.surfaceVariant

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) selectedBg else unselectedBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedGoal = goal }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = goal,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // 3. CLINICAL CONDITIONS
            // ---------------------------------------------------------
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Clinical Directives",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        conditionOptions.forEach { cond ->
                            val isSelected = selectedConditions.contains(cond)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedConditions = if (isSelected) selectedConditions - cond else selectedConditions + cond
                                },
                                label = { Text(cond, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // ---------------------------------------------------------
            // 4. SUBMIT BUTTON
            // ---------------------------------------------------------
            Button(
                onClick = {
                    isSubmitting = true
                    coroutineScope.launch {
                        val updated = currentProfile.copy(
                            age = ageText.toIntOrNull(),
                            heightCm = heightText.toDoubleOrNull(),
                            weightKg = weightText.toDoubleOrNull(),
                            fitnessGoal = selectedGoal,
                            healthConditions = selectedConditions.toList(),
                            isOnboardingCompleted = true
                        )
                        onCompleteOnboarding(updated)
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                } else {
                    Text(
                        text = "Save & Go to Dashboard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}