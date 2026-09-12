package com.example.nutriwise.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.nutriwise.domain.ReviewPost
import com.example.nutriwise.domain.UserProfile
import com.example.nutriwise.ui.auth.AuthViewModel
import com.example.nutriwise.ui.feed.FeedViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriWiseProfileScreen(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel,
    userPosts: List<ReviewPost>,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val userProfile by authViewModel.userProfile.collectAsState()
    val currentProfile = userProfile ?: UserProfile()

    ProfileScreenContent(
        initialProfile = currentProfile,
        userPosts = userPosts,
        isDarkMode = isDarkMode,
        onToggleTheme = onToggleTheme,
        onLogout = { authViewModel.logout() },
        onDeletePost = { post ->
            feedViewModel.deletePost(post.postId)
        },
        onSaveProfile = { updated ->
            authViewModel.saveProfileUpdates(updated)
        },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileScreenContent(
    initialProfile: UserProfile,
    userPosts: List<ReviewPost>,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onDeletePost: (ReviewPost) -> Unit,
    onSaveProfile: suspend (UserProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var saveSuccess by remember { mutableStateOf(false) }

    // Dynamic Conditions list combining defaults + any custom condition in user's profile
    val defaultConditions = remember {
        listOf(
            "Type 2 Diabetes", "Pre-Diabetes", "Hypertension (High BP)",
            "High Cholesterol", "Fatty Liver", "PCOS / PCOD",
            "Kidney Disease", "GERD / Acid Reflux", "Gluten Sensitivity"
        )
    }

    val defaultAllergens = remember {
        listOf(
            "Peanuts", "Soy / Soya", "Lactose / Dairy",
            "Tree Nuts", "Wheat / Gluten", "Mustard Seeds", "Sesame"
        )
    }

    // Editable active state
    var selectedConditions by remember(initialProfile.healthConditions) {
        mutableStateOf(initialProfile.healthConditions.toSet())
    }

    var customConditionInput by remember { mutableStateOf("") }
    var postToDelete by remember { mutableStateOf<ReviewPost?>(null) }

    // --- DISPLAY NAME & AUTO-SAVE STATE ---
    var displayName by remember(initialProfile.username) {
        mutableStateOf(initialProfile.username.ifBlank { "NutriWise User" })
    }
    var isEditingName by remember { mutableStateOf(false) }
    var isNameAutoSaving by remember { mutableStateOf(false) }
    var nameSaveSuccess by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Auto-save debouncer: automatically commits changes 800ms after user stops typing
    LaunchedEffect(displayName) {
        val trimmed = displayName.trim()
        if (trimmed.isNotBlank() && trimmed != initialProfile.username) {
            isNameAutoSaving = true
            nameSaveSuccess = false
            delay(800)
            onSaveProfile(initialProfile.copy(username = trimmed))
            isNameAutoSaving = false
            nameSaveSuccess = true
            delay(1500)
            nameSaveSuccess = false
        }
    }

    // Auto-focus text field when edit mode is toggled on
    LaunchedEffect(isEditingName) {
        if (isEditingName) {
            focusRequester.requestFocus()
        }
    }

    // Aggregate all conditions for chip display
    val allActiveConditions = remember(selectedConditions) {
        (defaultConditions + selectedConditions).distinct()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Clinical Health Profile",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // -------------------------------------------------------------
            // 1. PROFILE IDENTITY CARD (HOVER EDIT BUTTON & AUTO-SAVE)
            // -------------------------------------------------------------
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Dynamic Display Name with Edit Mode Toggle
                        if (!isEditingName) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { isEditingName = true }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = displayName,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Name",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .focusRequester(focusRequester),
                                    trailingIcon = {
                                        if (isNameAutoSaving) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        } else if (nameSaveSuccess) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Saved",
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        val trimmed = displayName.trim()
                                        if (trimmed.isNotBlank() && trimmed != initialProfile.username) {
                                            coroutineScope.launch {
                                                onSaveProfile(initialProfile.copy(username = trimmed))
                                            }
                                        }
                                        isEditingName = false
                                    },
                                    modifier = Modifier
                                        .size(46.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Confirm",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }

                        if (initialProfile.email.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = initialProfile.email,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProfileMetricBadge(
                                label = "Safety Mode",
                                value = if (selectedConditions.isEmpty()) "Standard" else "Custom",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileMetricBadge(
                                label = "My Reviews",
                                value = "${userPosts.size}",
                                modifier = Modifier.weight(1f)
                            )
                            ProfileMetricBadge(
                                label = "Directives",
                                value = "${selectedConditions.size}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. DYNAMIC HEALTH CONDITIONS
            // -------------------------------------------------------------
            item {
                SectionTitle(title = "Health Directives & Conditions", icon = Icons.Default.Favorite)
                Spacer(modifier = Modifier.height(8.dp))

                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "NutriWise AI tailors risk warnings against your active conditions. You can add or remove any custom diagnosis (e.g. GBS, Celiac).",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customConditionInput,
                                onValueChange = { customConditionInput = it },
                                placeholder = { Text("Add custom condition (e.g. GBS)", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    val trimmed = customConditionInput.trim()
                                    if (trimmed.isNotBlank()) {
                                        selectedConditions = selectedConditions + trimmed
                                        customConditionInput = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Condition",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allActiveConditions.forEach { condition ->
                                val isSelected = selectedConditions.contains(condition)
                                val isCustomCondition = !defaultConditions.contains(condition)

                                RemovableSelectableChip(
                                    text = condition,
                                    isSelected = isSelected,
                                    canDelete = isCustomCondition || isSelected,
                                    onToggle = {
                                        selectedConditions = if (isSelected) {
                                            selectedConditions - condition
                                        } else {
                                            selectedConditions + condition
                                        }
                                    },
                                    onDelete = {
                                        selectedConditions = selectedConditions - condition
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. COMMON ALLERGENS SELECTION
            // -------------------------------------------------------------
            item {
                SectionTitle(title = "Allergens & Triggers", icon = Icons.Default.Shield)
                Spacer(modifier = Modifier.height(8.dp))

                ElevatedCard(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            defaultAllergens.forEach { allergen ->
                                val isSelected = selectedConditions.contains(allergen)
                                SelectableChip(
                                    text = allergen,
                                    isSelected = isSelected,
                                    onToggle = {
                                        selectedConditions = if (isSelected) {
                                            selectedConditions - allergen
                                        } else {
                                            selectedConditions + allergen
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. SAVE PROFILE ACTION BUTTON
            // -------------------------------------------------------------
            item {
                Button(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val updated = initialProfile.copy(
                                    username = displayName.trim(),
                                    healthConditions = selectedConditions.toList()
                                )
                                onSaveProfile(updated)
                                saveSuccess = true
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = if (saveSuccess) "✓ Profile Saved & Synchronized" else "Save Clinical Profile",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. MY SHARED REVIEWS & POSTS
            // -------------------------------------------------------------
            item {
                Spacer(modifier = Modifier.height(6.dp))
                SectionTitle(title = "My Scanned Reviews (${userPosts.size})", icon = Icons.Default.RateReview)
                Spacer(modifier = Modifier.height(8.dp))

                if (userPosts.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "You haven't shared any product scans yet. Scan a food package and tap 'Share Review' to see it here.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(userPosts, key = { it.postId }) { post ->
                UserPostItemCard(
                    post = post,
                    onDelete = { postToDelete = post }
                )
            }
        }
    }

    // Confirmation Dialog for Post Deletion
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to remove '${postToDelete?.productName}' from your profile and community feed?") },
            confirmButton = {
                Button(
                    onClick = {
                        postToDelete?.let { onDeletePost(it) }
                        postToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// POST ITEM CARD WITH DELETE BUTTON
// -------------------------------------------------------------
@Composable
fun UserPostItemCard(
    post: ReviewPost,
    onDelete: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (post.packetImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = post.packetImageUrl,
                        contentDescription = post.productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.productName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Score: ${post.healthScore}/100 • ${post.verdict}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (post.healthScore >= 60) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Review",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (post.reviewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = post.reviewText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SELECTABLE & REMOVABLE CHIPS
// -------------------------------------------------------------
@Composable
fun RemovableSelectableChip(
    text: String,
    isSelected: Boolean,
    canDelete: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(200),
        label = "ChipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "ChipContent"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        modifier = modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = if (canDelete) 4.dp else 10.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )

            if (canDelete) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove Condition",
                        tint = contentColor.copy(alpha = 0.8f),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SelectableChip(
    text: String,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    RemovableSelectableChip(
        text = text,
        isSelected = isSelected,
        canDelete = false,
        onToggle = onToggle,
        onDelete = {},
        modifier = modifier
    )
}

@Composable
fun ProfileMetricBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SectionTitle(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}