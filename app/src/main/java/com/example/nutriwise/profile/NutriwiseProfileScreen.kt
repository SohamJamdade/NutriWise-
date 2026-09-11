package com.example.nutriwise.ui.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.nutriwise.domain.ReviewPost
import com.example.nutriwise.domain.UserProfile
import com.example.nutriwise.ui.auth.AuthViewModel
import com.example.nutriwise.ui.feed.FeedViewModel
import com.example.nutriwise.ui.feed.InstagramPostCard

@Composable
fun NutriWiseProfileScreen(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel = viewModel(),
    userPosts: List<ReviewPost> = emptyList(),
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user by authViewModel.userProfile.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = remember { listOf("Reviews", "Health Directives", "About") }

    var showEditDirectivesDialog by remember { mutableStateOf(false) }

    val totalDirectives = remember(user) {
        val u = user ?: return@remember 0
        (if (u.hasDiabetes) 1 else 0) +
                (if (u.hasHypertension) 1 else 0) +
                (if (u.hasHighCholesterol) 1 else 0) +
                (u.otherConditions.size)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // -------------------------------------------------------------
        // 1. BANNER & OVERLAPPING AVATAR
        // -------------------------------------------------------------
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                // Adaptive Banner Background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = if (isDarkMode) {
                                    listOf(Color(0xFF1E3A2F), Color(0xFF121E19), MaterialTheme.colorScheme.background)
                                } else {
                                    listOf(Color(0xFFC8E6C9), Color(0xFFA5D6A7), MaterialTheme.colorScheme.background)
                                }
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "u/${user?.username ?: "member"}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Theme Toggle Button
                            FilledIconButton(
                                onClick = onToggleTheme,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                    contentDescription = "Toggle Theme",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Logout Button
                            FilledIconButton(
                                onClick = { authViewModel.logout() },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                ),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Logout,
                                    contentDescription = "Logout",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(3.dp, MaterialTheme.colorScheme.background),
                        modifier = Modifier.size(88.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val imgUrl = user?.profileImageUrl
                            if (!imgUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                Text(
                                    text = (user?.username ?: "N").take(1).uppercase(),
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 2. USER DETAILS, BADGES & METRICS
        // -------------------------------------------------------------
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = user?.username ?: "Clean Eater",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedButton(
                        onClick = { showEditDirectivesDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Edit Directives", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Text(
                    text = "u/${user?.username ?: "member"} • ${user?.email ?: ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Scanning packages, avoiding industrial palm oils & decoding ultra-processed food.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Badges Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "🥗 Verified Scanner",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)),
                        modifier = Modifier.clickable { selectedTab = 1 }
                    ) {
                        Text(
                            text = "🛡️ $totalDirectives Active Directives >",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Surface
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileMetric(count = "${userPosts.size}", label = "Reviews")
                        ProfileMetric(count = "$totalDirectives", label = "Alerts")
                        ProfileMetric(count = "${user?.allergenAvoidList?.size ?: 0}", label = "Allergens")
                        ProfileMetric(count = "Tier 1", label = "Rank")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Header
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (selectedTab == index) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // 3. TAB CONTENT
        // -------------------------------------------------------------
        when (selectedTab) {
            0 -> {
                if (userPosts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No Community Reviews Yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Scanned products you post to feed appear here.",
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    items(userPosts, key = { post -> post.postId }) { post ->
                        InstagramPostCard(
                            post = post,
                            currentUserId = feedViewModel.currentUserId,
                            isLikedByMe = post.likedByUsers[feedViewModel.currentUserId] == true,
                            isDislikedByMe = false,
                            onProfileClick = {},
                            onLikeClick = { feedViewModel.toggleLike(post.postId) },
                            onDislikeClick = {},
                            onCommentClick = {},
                            onShareClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "NutriWise Review: ${post.productName} scored ${post.healthScore}/100 (${post.verdict}).\n${post.reviewText}"
                                    )
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Review"))
                            },
                            onDeleteClick = { feedViewModel.deletePost(post.postId) },
                            onEditClick = { /* Can trigger edit caption modal */ }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            1 -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Active Scanner Interceptors",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (user?.hasDiabetes == true) {
                            DirectiveCard(
                                title = "🩸 Diabetes / Blood Sugar Watch",
                                desc = "Scanner strictly flags maltodextrin, high dextrose, and added sugar spikes."
                            )
                        }
                        if (user?.hasHypertension == true) {
                            DirectiveCard(
                                title = "❤️ Hypertension & Sodium Watch",
                                desc = "Scanner checks high milligrams of sodium against clinical single-serving thresholds."
                            )
                        }
                        if (user?.hasHighCholesterol == true) {
                            DirectiveCard(
                                title = "🫀 Cholesterol / Palm Oil Watch",
                                desc = "Flags industrial palmolein fractions, trans fats, and hydrogenated oils."
                            )
                        }
                        user?.otherConditions?.forEach { condition ->
                            DirectiveCard(
                                title = "🩺 $condition Protocol",
                                desc = "Scanned items will prioritize warnings for $condition."
                            )
                        }

                        if (user?.allergenAvoidList?.isNotEmpty() == true) {
                            DirectiveCard(
                                title = "⚠️ Flagged Allergens",
                                desc = user?.allergenAvoidList?.joinToString(", ") ?: ""
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { showEditDirectivesDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Modify Health Directives", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            2 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Account Details",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Email: ${user?.email ?: "N/A"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Engine: NutriWise Clinical v2.4",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Evidence base: OpenFoodFacts + Gemini Multimodal Vision",
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------
    // 4. EDIT DIRECTIVES DIALOG
    // -------------------------------------------------------------
    val currentUser = user
    if (showEditDirectivesDialog && currentUser != null) {
        EditDirectivesDialog(
            user = currentUser,
            onDismiss = { showEditDirectivesDialog = false },
            onSave = { updatedUser ->
                authViewModel.saveProfileUpdates(updatedUser)
                showEditDirectivesDialog = false
            }
        )
    }
}

@Composable
private fun EditDirectivesDialog(
    user: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    var diabetes by remember(user) { mutableStateOf(user.hasDiabetes) }
    var hypertension by remember(user) { mutableStateOf(user.hasHypertension) }
    var cholesterol by remember(user) { mutableStateOf(user.hasHighCholesterol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "Edit Health Directives",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Toggle which health alerts the scanner must enforce for your profile:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                ThemeToggleRow("Diabetes / Blood Sugar", diabetes) { diabetes = it }
                ThemeToggleRow("Hypertension (High Sodium)", hypertension) { hypertension = it }
                ThemeToggleRow("High Cholesterol (Palm Oil)", cholesterol) { cholesterol = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        user.copy(
                            hasDiabetes = diabetes,
                            hasHypertension = hypertension,
                            hasHighCholesterol = cholesterol
                        )
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.outline)
            }
        }
    )
}

@Composable
private fun ProfileMetric(count: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun DirectiveCard(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ThemeToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}