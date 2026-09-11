package com.example.nutriwise.ui.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    var hasDiabetes by remember { mutableStateOf(false) }
    var hasHypertension by remember { mutableStateOf(false) }
    var hasHighCholesterol by remember { mutableStateOf(false) }
    var glutenAllergy by remember { mutableStateOf(false) }
    var peanutAllergy by remember { mutableStateOf(false) }
    var dairyAllergy by remember { mutableStateOf(false) }

    var otherCondition by remember { mutableStateOf("") }
    var customConditions by remember { mutableStateOf(listOf<String>()) }

    fun addCondition() {
        val trimmed = otherCondition.trim()
        if (trimmed.isNotEmpty() && !customConditions.any { it.equals(trimmed, ignoreCase = true) }) {
            customConditions = customConditions + trimmed
            otherCondition = ""
        }
    }

    // Google Sign In Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    viewModel.signInWithGoogle(credential)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) {
            onAuthSuccess()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("NutriWise", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
            Text(
                text = if (isSignUp) "Configure Clinical Health Profile" else "Clean Food Intelligence",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("YOUR_SERVER_CLIENT_ID_OR_DEFAULT")
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("🌐 Continue with Google", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            if (isSignUp) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (isSignUp) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    border = BorderStroke(1.dp, Color(0xFFDCEDC8)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🩺 Personalized Scanner Watchlists", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Scanned products will flag direct medical contraindications.", fontSize = 11.sp, color = Color.DarkGray)

                        Spacer(modifier = Modifier.height(12.dp))

                        HealthToggleRow("Diabetic / Sugar Watch", hasDiabetes) { hasDiabetes = it }
                        HealthToggleRow("Hypertension (High Sodium Watch)", hasHypertension) { hasHypertension = it }
                        HealthToggleRow("High Cholesterol / Palm Oil Watch", hasHighCholesterol) { hasHighCholesterol = it }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFDCEDC8))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("➕ Custom Condition (PCOS, Thyroid, etc.)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = otherCondition,
                            onValueChange = { otherCondition = it },
                            label = { Text("Add custom condition") },
                            placeholder = { Text("e.g. PCOS, Thyroid, GERD") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { addCondition() }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { addCondition() },
                            enabled = otherCondition.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF558B2F))
                        ) {
                            Text("Add Condition")
                        }

                        if (customConditions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                customConditions.forEach { condition ->
                                    InputChip(
                                        selected = true,
                                        onClick = { customConditions = customConditions - condition },
                                        label = { Text(condition) },
                                        trailingIcon = {
                                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0xFFDCEDC8))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("⚠️ Flag Allergens:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(selected = glutenAllergy, onClick = { glutenAllergy = !glutenAllergy }, label = { Text("Gluten") })
                            FilterChip(selected = peanutAllergy, onClick = { peanutAllergy = !peanutAllergy }, label = { Text("Peanuts") })
                            FilterChip(selected = dairyAllergy, onClick = { dairyAllergy = !dairyAllergy }, label = { Text("Dairy") })
                        }
                    }
                }
            }

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text((uiState as AuthUiState.Error).message, color = Color.Red, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isSignUp) {
                        val allergens = mutableListOf<String>()
                        if (glutenAllergy) allergens.add("Gluten")
                        if (peanutAllergy) allergens.add("Peanuts")
                        if (dairyAllergy) allergens.add("Dairy")

                        viewModel.signUpWithHealthToggles(
                            email = email.trim(),
                            pass = password.trim(),
                            username = username.trim(),
                            hasDiabetes = hasDiabetes,
                            hasHypertension = hasHypertension,
                            hasHighCholesterol = hasHighCholesterol,
                            allergens = allergens,
                            otherConditions = customConditions
                        )
                    } else {
                        viewModel.login(email.trim(), password.trim())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                enabled = uiState !is AuthUiState.Loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text(if (isSignUp) "Create Account & Start Scanning" else "Sign In", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { isSignUp = !isSignUp }) {
                Text(
                    text = if (isSignUp) "Already have an account? Sign In" else "New to NutriWise? Set up Health Profile",
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun HealthToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, color = Color.Black, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF2E7D32)))
    }
}