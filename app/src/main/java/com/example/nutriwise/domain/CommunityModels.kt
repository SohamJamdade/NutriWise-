package com.example.nutriwise.domain

import com.google.firebase.database.IgnoreExtraProperties


data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val profileImageUrl: String = "",
    val hasDiabetes: Boolean = false,
    val hasHypertension: Boolean = false,
    val hasHighCholesterol: Boolean = false,
    val isHighProteinGoal: Boolean = false,
    val allergenAvoidList: List<String> = emptyList(),
    val otherConditions: List<String> = emptyList()
)

@IgnoreExtraProperties
data class PostComment(
    var commentId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var authorAvatarUrl: String = "",
    var text: String = "",
    var timestamp: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class ReviewPost(
    var postId: String = "",
    var authorUid: String = "",
    var authorName: String = "",
    var authorAvatarUrl: String = "",
    var productName: String = "",
    var healthScore: Int = 0,
    var verdict: String = "",
    var reviewText: String = "",
    var packetImageUrl: String = "",
    var suggestedSwap: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var likesCount: Int = 0,
    var likedByUsers: Map<String, Boolean> = emptyMap(),
    var dislikesCount: Int = 0,
    var dislikedByUsers: Map<String, Boolean> = emptyMap(),
    var comments: Map<String, PostComment> = emptyMap()
)