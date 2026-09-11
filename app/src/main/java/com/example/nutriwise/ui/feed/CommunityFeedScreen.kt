package com.example.nutriwise.ui.feed

import android.content.Intent
import android.graphics.BitmapFactory
import android.text.format.DateUtils
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.nutriwise.domain.PostComment
import com.example.nutriwise.domain.ReviewPost
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedScreen(
    feedViewModel: FeedViewModel = viewModel(),
    isDarkMode: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onProfileClick: () -> Unit
) {
    val state by feedViewModel.feedState.collectAsState()
    val currentUid = feedViewModel.currentUserId
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("All") }

    var editingPost by remember { mutableStateOf<ReviewPost?>(null) }
    var editedText by remember { mutableStateOf("") }
    var activeCommentingPostId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NutriWise", fontWeight = FontWeight.Black, fontSize = 22.sp, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("COMMUNITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Interactive Filter Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = selectedFilter == "All", onClick = { selectedFilter = "All" }, label = { Text("🌟 All Reviews") })
                FilterChip(selected = selectedFilter == "Healthy", onClick = { selectedFilter = "Healthy" }, label = { Text("🥗 Clean Foods (>70)") })
                FilterChip(selected = selectedFilter == "Unhealthy", onClick = { selectedFilter = "Unhealthy" }, label = { Text("⚠️ Ultra-Processed (<45)") })
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            when (val ui = state) {
                is FeedUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is FeedUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(ui.message, color = Color.Red, fontSize = 14.sp)
                    }
                }
                is FeedUiState.Success -> {
                    val filtered = remember(ui.posts, selectedFilter) {
                        when (selectedFilter) {
                            "Healthy" -> ui.posts.filter { it.healthScore >= 70 }
                            "Unhealthy" -> ui.posts.filter { it.healthScore < 45 }
                            else -> ui.posts
                        }
                    }

                    if (filtered.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No reviews match this filter.", color = Color.Gray, fontSize = 14.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.postId }) { post ->
                                InstagramPostCard(
                                    post = post,
                                    currentUserId = currentUid,
                                    isLikedByMe = post.likedByUsers[currentUid] == true,
                                    isDislikedByMe = post.dislikedByUsers[currentUid] == true,
                                    onProfileClick = onProfileClick,
                                    onLikeClick = { feedViewModel.toggleLike(post.postId) },
                                    onDislikeClick = { feedViewModel.toggleDislike(post.postId) },
                                    onCommentClick = { activeCommentingPostId = post.postId },
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
                                    onEditClick = {
                                        editingPost = post
                                        editedText = post.reviewText
                                    }
                                )
                            }
                        }
                    }

                    // YouTube-Style Bottom Sheet
                    activeCommentingPostId?.let { targetId ->
                        val targetPost = ui.posts.find { it.postId == targetId }
                        if (targetPost != null) {
                            YouTubeStyleCommentsSheet(
                                post = targetPost,
                                onDismiss = { activeCommentingPostId = null },
                                onSendComment = { commentText ->
                                    feedViewModel.addComment(targetId, commentText)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingPost != null) {
        AlertDialog(
            onDismissRequest = { editingPost = null },
            title = { Text("Edit Your Review", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Your thoughts on ${editingPost?.productName}") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingPost?.let { feedViewModel.editPostDescription(it.postId, editedText) }
                        editingPost = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPost = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun InstagramPostCard(
    post: ReviewPost,
    currentUserId: String?,
    isLikedByMe: Boolean,
    isDislikedByMe: Boolean,
    onProfileClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    var showHeartOverlay by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val isOwner = currentUserId != null && currentUserId == post.authorUid

    LaunchedEffect(showHeartOverlay) {
        if (showHeartOverlay) {
            delay(800)
            showHeartOverlay = false
        }
    }

    Card(
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { onProfileClick() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = post.authorName.take(1).uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onProfileClick() }
                ) {
                    Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    val dateStr = SimpleDateFormat("dd MMM • hh:mm a", Locale.getDefault()).format(Date(post.timestamp))
                    Text(dateStr, fontSize = 11.sp, color = Color.Gray)
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = when {
                        post.healthScore >= 70 -> Color(0xFF2E7D32)
                        post.healthScore >= 45 -> Color(0xFFF57F17)
                        else -> Color(0xFFC62828)
                    }
                ) {
                    Text(
                        text = "Score: ${post.healthScore}/100",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        if (isOwner) {
                            DropdownMenuItem(
                                text = { Text("Edit Caption") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = Color.Red) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Report Post") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = { menuExpanded = false }
                            )
                        }
                    }
                }
            }

            // Image Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .background(Color(0xFF1E1E24))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (!isLikedByMe) onLikeClick()
                                showHeartOverlay = true
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                PostImageRenderer(
                    imageUrl = post.packetImageUrl,
                    productName = post.productName,
                    modifier = Modifier.fillMaxSize()
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = showHeartOverlay,
                    enter = scaleIn(animationSpec = tween(200)),
                    exit = scaleOut(animationSpec = tween(300))
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(90.dp)
                    )
                }
            }

            // Action Strip (YouTube-style segmented reaction pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Segmented Pill for Likes & Dislikes
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        // Like Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onLikeClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isLikedByMe) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "Like",
                                tint = if (isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${post.likesCount}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLikedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Divider between Like & Dislike
                        VerticalDivider(
                            modifier = Modifier
                                .height(18.dp)
                                .padding(horizontal = 2.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )

                        // Dislike Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onDislikeClick() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (isDislikedByMe) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                contentDescription = "Dislike",
                                tint = if (isDislikedByMe) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            if (post.dislikesCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${post.dislikesCount}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDislikedByMe) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // YouTube-Style Comment Capsule
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onCommentClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Default.ChatBubbleOutline,
                            contentDescription = "Comments",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.comments.size}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Share Button Capsule
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onShareClick() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Post Description Body
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                Row {
                    Text(post.authorName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(post.productName, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(post.reviewText, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (post.suggestedSwap.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🥗", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cleaner Swap: ${post.suggestedSwap}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// -------------------------------------------------------------
// YOUTUBE-STYLE COMMENTS SHEET
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubeStyleCommentsSheet(
    post: ReviewPost,
    onDismiss: () -> Unit,
    onSendComment: (String) -> Unit
) {
    var newCommentText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val commentsList = remember(post.comments) {
        post.comments.values.toList().sortedByDescending { it.timestamp }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(2.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(width = 38.dp, height = 4.dp)
                ) {}
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Comments ${post.comments.size}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))

            // Comments List Area
            if (commentsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Forum,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No comments yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Be the first to share your thoughts.", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(commentsList, key = { it.commentId }) { comment ->
                        YouTubeCommentItem(comment)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))

            // Bottom Input Dock (Docked above keyboard)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Add a comment...", fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                onSendComment(newCommentText)
                                newCommentText = ""
                            }
                        },
                        enabled = newCommentText.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (newCommentText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeCommentItem(comment: PostComment) {
    val relativeTime = remember(comment.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            comment.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Author Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (comment.authorAvatarUrl.isNotBlank()) {
                    AsyncImage(
                        model = comment.authorAvatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = comment.authorName.take(1).uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Author Name, Timestamp & Comment Content
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "@${comment.authorName}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = relativeTime,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = comment.text,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PostImageRenderer(imageUrl: String, productName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val isBase64 = remember(imageUrl) {
        imageUrl.startsWith("data:image") || (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://") && imageUrl.length > 100)
    }

    if (isBase64) {
        val bitmap = remember(imageUrl) {
            try {
                val cleanBase64 = if (imageUrl.contains(",")) imageUrl.substringAfter(",") else imageUrl
                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                null
            }
        }

        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = productName,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        } else {
            ImageFallback(productName, modifier)
        }
    } else if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = productName,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        ImageFallback(productName, modifier)
    }
}

@Composable
private fun ImageFallback(productName: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Color(0xFF1E1E24)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Fastfood, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(54.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(productName, color = Color.LightGray, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}