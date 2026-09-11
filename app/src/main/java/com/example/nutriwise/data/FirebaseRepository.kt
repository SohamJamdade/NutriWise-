package com.example.nutriwise.data

import com.example.nutriwise.domain.ReviewPost
import com.example.nutriwise.domain.UserProfile
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import android.graphics.Bitmap
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID
import android.graphics.Matrix
import android.util.Base64
import com.example.nutriwise.domain.PostComment
import kotlin.math.max


class FirebaseRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val usersRef = db.getReference("users")
    private val postsRef = db.getReference("posts")
    private val storage = FirebaseStorage.getInstance()

    private val storageRef = storage.reference.child("post_images")

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun signInWithGoogleCredential(credential: AuthCredential): Result<UserProfile> {
        return try {
            val authResult = auth.signInWithCredential(credential).await()
            val firebaseUser = authResult.user ?: throw IllegalStateException("Google Auth Failed")
            val uid = firebaseUser.uid

            val snapshot = usersRef.child(uid).get().await()
            val existingProfile = snapshot.getValue(UserProfile::class.java)

            val profileToSave = existingProfile ?: UserProfile(
                uid = uid,
                username = firebaseUser.displayName ?: "NutriWise User",
                email = firebaseUser.email ?: "",
                profileImageUrl = firebaseUser.photoUrl?.toString() ?: ""
            )

            if (existingProfile == null) {
                usersRef.child(uid).setValue(profileToSave).await()
            }

            Result.success(profileToSave)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(
        email: String,
        pass: String,
        username: String,
        profile: UserProfile
    ): Result<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid =
                authResult.user?.uid ?: throw IllegalStateException("User ID generation failed")
            val userRecord = profile.copy(uid = uid, username = username, email = email)
            usersRef.child(uid).setValue(userRecord).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, pass: String): Result<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }

    suspend fun fetchCurrentUserProfile(): UserProfile? {
        val uid = currentUserId ?: return null
        return try {
            val snapshot = usersRef.child(uid).get().await()
            snapshot.getValue(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateHealthProfile(profile: UserProfile): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            usersRef.child(uid).setValue(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REALTIME FEED FLOW ---
    fun observeCommunityFeed(): Flow<List<ReviewPost>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<ReviewPost>()
                for (child in snapshot.children) {
                    child.getValue(ReviewPost::class.java)?.let { posts.add(it) }
                }
                // Sort by newest post first
                trySend(posts.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        postsRef.addValueEventListener(listener)
        awaitClose { postsRef.removeEventListener(listener) }
    }

    fun convertBitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 600
        val width = bitmap.width
        val height = bitmap.height
        val longest = max(width, height)

        val scaled = if (longest > maxDimension) {
            val ratio = maxDimension.toFloat() / longest.toFloat()
            val matrix = Matrix().apply { postScale(ratio, ratio) }
            Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
        } else {
            bitmap

        }
        // Compress to 60% JPEG
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        val bytes = stream.toByteArray()

        // Format as Base64 image data URI
        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$encoded"
    }

    // --- USER SPECIFIC POSTS (FOR PROFILE TAB) ---
    // Queries only posts matching current user's UID so Profile -> Reviews populates immediately
    fun observeUserPosts(uid: String): Flow<List<ReviewPost>> = callbackFlow {
        val query = postsRef.orderByChild("authorUid").equalTo(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val posts = mutableListOf<ReviewPost>()
                for (child in snapshot.children) {
                    child.getValue(ReviewPost::class.java)?.let { posts.add(it) }
                }
                trySend(posts.sortedByDescending { it.timestamp })
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun uploadCapturedPhoto(bitmap: Bitmap): Result<String> {
        return try {
            val imageId = UUID.randomUUID().toString()
            val fileRef = storageRef.child("$imageId.jpg")

            // Compress bitmap to 80% JPEG to balance quality and speed
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val data = baos.toByteArray()

            // Upload and await result
            fileRef.putBytes(data).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createReviewPost(
        productName: String,
        healthScore: Int,
        verdict: String,
        reviewText: String,
        packetImageUrl: String,
        suggestedSwap: String
    ): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Must be logged in"))
        val user = fetchCurrentUserProfile()
        val postId = postsRef.push().key
            ?: return Result.failure(IllegalStateException("Failed key generation"))

        val newPost = ReviewPost(
            postId = postId,
            authorUid = uid,
            authorName = user?.username ?: "NutriWise Foodie",
            authorAvatarUrl = user?.profileImageUrl ?: "",
            productName = productName,
            healthScore = healthScore,
            verdict = verdict,
            reviewText = reviewText,
            packetImageUrl = packetImageUrl,
            suggestedSwap = suggestedSwap,
            timestamp = System.currentTimeMillis()
        )

        return try {
            postsRef.child(postId).setValue(newPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REMOTE DELETE POST ---
    // Security check: Ensures only the author can delete their post
    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            val snapshot = postsRef.child(postId).get().await()
            val post = snapshot.getValue(ReviewPost::class.java) ?: return Result.failure(
                IllegalStateException("Post not found")
            )

            if (post.authorUid != uid) {
                return Result.failure(SecurityException("Unauthorized: You can only delete your own reviews"))
            }

            postsRef.child(postId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REMOTE EDIT POST CAPTION / OPINION ---
    suspend fun updatePostDescription(postId: String, newText: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            val snapshot = postsRef.child(postId).get().await()
            val post = snapshot.getValue(ReviewPost::class.java) ?: return Result.failure(
                IllegalStateException("Post not found")
            )

            if (post.authorUid != uid) {
                return Result.failure(SecurityException("Unauthorized: You can only edit your own reviews"))
            }

            postsRef.child(postId).child("reviewText").setValue(newText).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // In data/FirebaseRepository.kt

    /**
     * Toggles Like with YouTube-style mutual exclusion.
     * - If liked -> removes like.
     * - If disliked -> removes dislike and sets like.
     * - If neutral -> sets like.
     */
    suspend fun togglePostLike(postId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        val postRef = postsRef.child(postId)

        return try {
            val snapshot = postRef.get().await()
            val post = snapshot.getValue(ReviewPost::class.java)
                ?: return Result.failure(IllegalStateException("Post not found"))

            val likedMap = post.likedByUsers.toMutableMap()
            val dislikedMap = post.dislikedByUsers.toMutableMap()

            if (likedMap[uid] == true) {
                // User un-likes the post (returns to neutral)
                likedMap.remove(uid)
            } else {
                // Add like & strip any active dislike
                likedMap[uid] = true
                dislikedMap.remove(uid)
            }

            val updates = mapOf(
                "likedByUsers" to likedMap,
                "likesCount" to likedMap.size,
                "dislikedByUsers" to dislikedMap,
                "dislikesCount" to dislikedMap.size
            )

            postRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Toggles Dislike with YouTube-style mutual exclusion.
     * - If disliked -> removes dislike.
     * - If liked -> removes like and sets dislike.
     * - If neutral -> sets dislike.
     */
    suspend fun togglePostDislike(postId: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        val postRef = postsRef.child(postId)

        return try {
            val snapshot = postRef.get().await()
            val post = snapshot.getValue(ReviewPost::class.java)
                ?: return Result.failure(IllegalStateException("Post not found"))

            val likedMap = post.likedByUsers.toMutableMap()
            val dislikedMap = post.dislikedByUsers.toMutableMap()

            if (dislikedMap[uid] == true) {
                // User un-dislikes the post (returns to neutral)
                dislikedMap.remove(uid)
            } else {
                // Add dislike & strip any active like
                dislikedMap[uid] = true
                likedMap.remove(uid)
            }

            val updates = mapOf(
                "likedByUsers" to likedMap,
                "likesCount" to likedMap.size,
                "dislikedByUsers" to dislikedMap,
                "dislikesCount" to dislikedMap.size
            )

            postRef.updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a comment including the user's username and profile picture.
     */
    suspend fun addPostComment(postId: String, commentText: String): Result<Unit> {
        val uid = currentUserId ?: return Result.failure(IllegalStateException("Not logged in"))
        val user = fetchCurrentUserProfile()
        val commentsRef = postsRef.child(postId).child("comments")
        val commentId = commentsRef.push().key
            ?: return Result.failure(IllegalStateException("Key generation failed"))

        val comment = PostComment(
            commentId = commentId,
            authorUid = uid,
            authorName = user?.username ?: "Foodie",
            authorAvatarUrl = user?.profileImageUrl ?: "",
            text = commentText.trim(),
            timestamp = System.currentTimeMillis()
        )

        return try {
            commentsRef.child(commentId).setValue(comment).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}//
