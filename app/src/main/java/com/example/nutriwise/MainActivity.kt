package com.example.nutriwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriwise.ui.AnalysisResultScreen
import com.example.nutriwise.ui.CameraScannerView
import com.example.nutriwise.ui.ScanUiState
import com.example.nutriwise.ui.ScannerViewModel
import com.example.nutriwise.ui.auth.AuthScreen
import com.example.nutriwise.ui.auth.AuthUiState
import com.example.nutriwise.ui.auth.AuthViewModel
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

            NutriWiseTheme(darkTheme = isDarkMode) {
                val authViewModel: AuthViewModel = viewModel()
                val authState by authViewModel.uiState.collectAsState()

                if (authState !is AuthUiState.Authenticated) {
                    AuthScreen(
                        viewModel = authViewModel,
                        onAuthSuccess = { authViewModel.loadUserProfile() }
                    )
                } else {
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

@Composable
fun MainAppScaffold(
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val scannerViewModel: ScannerViewModel = viewModel()
    val feedViewModel: FeedViewModel = viewModel()

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
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        else -> {
                            CameraScannerView(
                                onImageCaptured = { bitmap -> scannerViewModel.processCapturedImage(bitmap) }
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