package com.example.nutriwise

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriwise.domain.UserProfile
import com.example.nutriwise.ui.AnalysisResultScreen
import com.example.nutriwise.ui.CameraScannerView
import com.example.nutriwise.ui.ScanUiState
import com.example.nutriwise.ui.ScannerViewModel
import com.example.nutriwise.ui.auth.AuthScreen
import com.example.nutriwise.ui.auth.AuthUiState
import com.example.nutriwise.ui.auth.AuthViewModel
import com.example.nutriwise.ui.auth.HealthOnboardingScreen
import com.example.nutriwise.ui.feed.CommunityFeedScreen
import com.example.nutriwise.ui.feed.FeedViewModel
import com.example.nutriwise.ui.profile.NutriWiseProfileScreen
import com.example.nutriwise.ui.theme.NutriWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by rememberSaveable { mutableStateOf(systemDark) }
            val context = LocalContext.current

            val prefs = remember { context.getSharedPreferences("nutriwise_prefs", Context.MODE_PRIVATE) }

            NutriWiseTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.uiState.collectAsState()
                val userProfile by authViewModel.userProfile.collectAsState()
                val currentUser = authViewModel.currentUser

                // User-scoped onboarding key so a new account doesn't inherit an old user's completion status
                val userOnboardingKey = remember(currentUser?.uid) {
                    "is_onboarding_completed_${currentUser?.uid ?: "none"}"
                }
                var isOnboardingDone by remember(currentUser?.uid) {
                    mutableStateOf(prefs.getBoolean(userOnboardingKey, false))
                }

                // Automatically load user profile whenever an account signs in
                LaunchedEffect(currentUser?.uid) {
                    if (currentUser != null && userProfile == null) {
                        authViewModel.loadUserProfile()
                    }
                }

                when {
                    // 1. Not Authenticated -> Show Login / Register
                    authState !is AuthUiState.Authenticated || currentUser == null -> {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = { authViewModel.loadUserProfile() }
                        )
                    }

                    // 2. Authenticated but waiting for profile data from Firebase -> Show Spinner
                    userProfile == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // 3. User has NOT completed onboarding (checked both locally and against Firebase)
                    !isOnboardingDone && userProfile?.isOnboardingCompleted != true -> {
                        HealthOnboardingScreen(
                            currentProfile = userProfile ?: UserProfile(),
                            onCompleteOnboarding = { updatedProfile ->
                                // Save completion specifically for this UID
                                prefs.edit().putBoolean(userOnboardingKey, true).apply()
                                isOnboardingDone = true
                                authViewModel.saveProfileUpdates(updatedProfile)
                            }
                        )
                    }

                    // 4. Fully Onboarded -> Main Dashboard
                    else -> {
                        MainAppScaffold(
                            authViewModel = authViewModel,
                            isDarkMode = isDarkMode,
                            onToggleTheme = { isDarkMode = !isDarkMode }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val scannerViewModel: ScannerViewModel = viewModel()
    val feedViewModel: FeedViewModel = viewModel()

    val userProfile by authViewModel.userProfile.collectAsState()
    val scanState by scannerViewModel.uiState.collectAsState()
    val myPosts by feedViewModel.myPostsState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.DynamicFeed, contentDescription = "Feed") },
                    label = { Text("Feed") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
                    label = { Text("Scanner") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedTab) {
                0 -> CommunityFeedScreen(
                    feedViewModel = feedViewModel,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme,
                    onProfileClick = { selectedTab = 2 }
                )
                1 -> {
                    when (val state = scanState) {
                        is ScanUiState.Success -> {
                            AnalysisResultScreen(
                                analysis = state.analysis,
                                capturedBitmap = state.capturedBitmap,
                                onScanAgain = { scannerViewModel.resetScan() }
                            )
                        }
                        is ScanUiState.Processing -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.padding(top = 16.dp))
                                    Text(
                                        text = "Analyzing nutrition label with AI...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                        is ScanUiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Error",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Scan Issue",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = state.message,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = { scannerViewModel.resetScan() },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Try Again")
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            CameraScannerView(
                                onImageCaptured = { bitmap -> scannerViewModel.processCapturedImage(bitmap, userProfile) }
                            )
                        }
                    }
                }
                2 -> NutriWiseProfileScreen(
                    authViewModel = authViewModel,
                    feedViewModel = feedViewModel,
                    userPosts = myPosts,
                    isDarkMode = isDarkMode,
                    onToggleTheme = onToggleTheme
                )
            }
        }
    }
}