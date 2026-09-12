package com.example.nutriwise.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriwise.domain.UserProfile

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthSurveyScreen(
    currentProfile: UserProfile,
    onSurveyComplete: (List<String>) -> Unit
) {
    // Preset suggestions for quick tap
    val suggestions = listOf(
        "Diabetes / Blood Sugar", "Hypertension (High BP)", "High Cholesterol",
        "PCOS", "Thyroid", "Uric Acid / Gout", "Lactose Intolerant",
        "Gluten Sensitivity / Celiac", "Nut Allergy", "GERD / Acid Reflux"
    )

    var selectedConditions by remember { mutableStateOf(currentProfile.healthConditions.toSet()) }
    var customInput by remember { mutableStateOf("") }

    fun addCustomCondition() {
        val trimmed = customInput.trim()
        if (trimmed.isNotBlank()) {
            selectedConditions = selectedConditions + trimmed
            customInput = ""
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Icon(
                imageVector = Icons.Default.HealthAndSafety,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tailor Your Food Scanner",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Select or type any diseases, conditions, or allergies. Whenever you scan an ingredient label, NutriWise will actively cross-check it for you.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Custom condition text field
            Text(
                text = "Add Custom Condition or Allergy",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { customInput = it },
                    placeholder = { Text("e.g. Histamine intolerance, Shellfish, IBS") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { addCustomCondition() }),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { addCustomCondition() },
                    shape = RoundedCornerShape(12.dp),
                    enabled = customInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Active tags selected
            Text(
                text = "Your Active Directives (${selectedConditions.size})",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedConditions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No restrictions selected. You can tap common options below or tap skip.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (condition in selectedConditions) {
                        InputChip(
                            selected = true,
                            onClick = { selectedConditions = selectedConditions - condition },
                            label = { Text(condition, fontWeight = FontWeight.SemiBold) },
                            trailingIcon = {
                                Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                            },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Select Suggestions
            Text(
                text = "Common Health Concerns",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (item in suggestions) {
                    val isSelected = selectedConditions.contains(item)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedConditions = if (isSelected) {
                                selectedConditions - item
                            } else {
                                selectedConditions + item
                            }
                        },
                        label = { Text(item, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Save & Continue Button
            Button(
                onClick = { onSurveyComplete(selectedConditions.toList()) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = if (selectedConditions.isNotEmpty()) "Save Profile & Start Scanning" else "Continue Without Conditions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}