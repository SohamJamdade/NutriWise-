package com.example.nutriwise.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nutriwise.domain.ReviewPost
import com.example.nutriwise.domain.UserProfile
import com.example.nutriwise.ui.auth.AuthViewModel
import com.example.nutriwise.ui.feed.FeedViewModel
import com.example.nutriwise.ui.feed.PostImageView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NutriWiseProfileScreen(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel,
    userPosts: List<ReviewPost>,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val userProfile by authViewModel.userProfile.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<ReviewPost?>(null) }

    val profile = userProfile ?: UserProfile()

    // Health directives state
    var customConditionInput by remember { mutableStateOf("") }
    var selectedConditions by remember(profile.healthConditions) {
        mutableStateOf(profile.healthConditions.toSet())
    }

    val defaultConditions = listOf(
        "Type 2 Diabetes", "Pre-Diabetes", "Hypertension (High BP)",
        "High Cholesterol", "Fatty Liver", "PCOS / PCOD",
        "Kidney Disease", "GERD / Acid Reflux", "Gluten Sensitivity"
    )
    val allConditions = remember(selectedConditions) {
        (defaultConditions + selectedConditions).distinct()
    }

    var isSavingDirectives by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Header Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.profileImageUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(profile.profileImageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Profile Picture",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.username.ifBlank { "NutriWise User" },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = profile.email.ifBlank { "No email provided" },
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit Profile",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. Health & Goals Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Health & Fitness Details",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            TextButton(onClick = { showEditDialog = true }) {
                                Text("Edit All")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProfileStatBox(
                                label = "Goal",
                                value = profile.fitnessGoal.ifBlank { "Clean Eating" },
                                modifier = Modifier.weight(1.1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProfileStatBox(
                                label = "Age",
                                value = profile.age?.let { "$it yrs" } ?: "--",
                                modifier = Modifier.weight(0.7f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProfileStatBox(
                                label = "Weight",
                                value = profile.weightKg?.let { "$it kg" } ?: "--",
                                modifier = Modifier.weight(0.8f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProfileStatBox(
                                label = "Height",
                                value = profile.heightCm?.let { "$it cm" } ?: "--",
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Active Health Directives:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Custom Directive Input Field
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = customConditionInput,
                                onValueChange = { customConditionInput = it },
                                placeholder = { Text("Add custom condition (e.g. Celiac)", fontSize = 12.sp) },
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Conditions Chip Cloud
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            allConditions.forEach { condition ->
                                val isSelected = selectedConditions.contains(condition)
                                val isCustom = !defaultConditions.contains(condition)

                                RemovableSelectableChip(
                                    text = condition,
                                    isSelected = isSelected,
                                    canDelete = isCustom || isSelected,
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

                        // Save Condition Changes Button (if modified)
                        if (selectedConditions != profile.healthConditions.toSet()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    isSavingDirectives = true
                                    coroutineScope.launch {
                                        try {
                                            val updated = profile.copy(
                                                healthConditions = selectedConditions.toList()
                                            )
                                            authViewModel.saveProfileUpdates(updated)
                                        } finally {
                                            isSavingDirectives = false
                                        }
                                    }
                                },
                                enabled = !isSavingDirectives,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isSavingDirectives) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                } else {
                                    Text("Save Directive Changes")
                                }
                            }
                        }
                    }
                }
            }

            // 3. User Posts Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Reviews & Scans (${userPosts.size})",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // 4. User Posts List or Empty State
            if (userPosts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DynamicFeed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No shared reviews yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Scan food labels and publish reviews to see them here.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(userPosts, key = { it.postId }) { post ->
                    UserPostCard(
                        post = post,
                        onDeleteClick = { postToDelete = post }
                    )
                }
            }
        }
    }

    // Dialog for Editing Full Profile
    if (showEditDialog) {
        EditProfileDialog(
            currentProfile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { updatedProfile ->
                authViewModel.saveProfileUpdates(updatedProfile)
                showEditDialog = false
            }
        )
    }

    // Dialog for Post Deletion
    postToDelete?.let { post ->
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Review") },
            text = { Text("Are you sure you want to delete your review for '${post.productName}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        feedViewModel.deletePost(post.postId)
                        postToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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

@Composable
fun RemovableSelectableChip(
    text: String,
    isSelected: Boolean,
    canDelete: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = { Text(text, fontSize = 12.sp) },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(14.dp)
                )
            }
        } else null,
        trailingIcon = if (canDelete) {
            {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove Condition",
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onDelete() }
                )
            }
        } else null,
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun ProfileStatBox(
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
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun UserPostCard(
    post: ReviewPost,
    onDeleteClick: () -> Unit
) {
    val scoreColor = when {
        post.healthScore >= 70 -> Color(0xFF2E7D32)
        post.healthScore >= 45 -> Color(0xFFF57C00)
        else -> Color(0xFFD32F2F)
    }

    val formattedDate = remember(post.timestamp) {
        try {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(Date(post.timestamp))
        } catch (_: Exception) {
            ""
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.productName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (formattedDate.isNotBlank()) {
                        Text(
                            text = formattedDate,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scoreColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor)
                ) {
                    Text(
                        text = "${post.healthScore} / 100",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Post",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (post.packetImageUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                PostImageView(
                    imageUrl = post.packetImageUrl,
                    contentDescription = post.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }

            if (post.verdict.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Verdict: ${post.verdict}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (post.reviewText.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.reviewText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditProfileDialog(
    currentProfile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (UserProfile) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf(currentProfile.username) }
    var editAge by remember { mutableStateOf(currentProfile.age?.toString() ?: "") }
    var editHeight by remember { mutableStateOf(currentProfile.heightCm?.toString() ?: "") }
    var editWeight by remember { mutableStateOf(currentProfile.weightKg?.toString() ?: "") }
    var editGoal by remember { mutableStateOf(currentProfile.fitnessGoal.ifBlank { "Maintain & Clean Eating" }) }

    val defaultOptions = listOf(
        "Type 2 Diabetes", "Hypertension (High BP)", "High Cholesterol",
        "Fatty Liver", "PCOS / PCOD", "GERD / Acid Reflux", "GBS"
    )
    var selectedConditions by remember { mutableStateOf(currentProfile.healthConditions.toSet()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Health Profile") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = editAge,
                        onValueChange = { editAge = it.filter { char -> char.isDigit() } },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editWeight,
                        onValueChange = { editWeight = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = editHeight,
                        onValueChange = { editHeight = it },
                        label = { Text("Height (cm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Fitness Goal:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                val goals = listOf(
                    "Weight Loss / Fat Cut",
                    "Weight Gain / Muscle Building",
                    "Maintain & Clean Eating",
                    "Athletic Performance"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    goals.forEach { goal ->
                        FilterChip(
                            selected = editGoal == goal,
                            onClick = { editGoal = goal },
                            label = { Text(goal, fontSize = 12.sp) }
                        )
                    }
                }

                Text("Health Directives:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    (defaultOptions + selectedConditions).distinct().forEach { condition ->
                        FilterChip(
                            selected = selectedConditions.contains(condition),
                            onClick = {
                                selectedConditions = if (selectedConditions.contains(condition)) {
                                    selectedConditions - condition
                                } else {
                                    selectedConditions + condition
                                }
                            },
                            label = { Text(condition, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSaving = true
                    coroutineScope.launch {
                        try {
                            val updated = currentProfile.copy(
                                username = displayName.trim(),
                                age = editAge.toIntOrNull(),
                                heightCm = editHeight.toDoubleOrNull(),
                                weightKg = editWeight.toDoubleOrNull(),
                                fitnessGoal = editGoal,
                                healthConditions = selectedConditions.toList(),
                                isOnboardingCompleted = true
                            )
                            onSave(updated)
                        } finally {
                            isSaving = false
                        }
                    }
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Cancel")
            }
        }
    )
}