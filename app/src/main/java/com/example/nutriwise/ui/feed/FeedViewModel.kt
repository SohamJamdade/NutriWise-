package com.example.nutriwise.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriwise.data.FirebaseRepository
import com.example.nutriwise.domain.ReviewPost
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface FeedUiState {
    object Loading : FeedUiState
    data class Success(val posts: List<ReviewPost>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}

// Fixed class name casing: FeedViewModel (capital 'V')
class FeedViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    // Fixed property name: currentUserId (matching references across screens)
    val currentUserId: String? = repository.currentUserId

    val feedState: StateFlow<FeedUiState> = repository.observeCommunityFeed()
        .map<List<ReviewPost>, FeedUiState> { posts -> FeedUiState.Success(posts) }
        .catch { emit(FeedUiState.Error(it.localizedMessage ?: "Failed to load posts")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = FeedUiState.Loading
        )

    val myPostsState: StateFlow<List<ReviewPost>> = flow {
        val uid = repository.currentUserId
        if (uid != null) {
            emitAll(repository.observeUserPosts(uid))
        } else {
            emit(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun toggleLike(postId: String) {
        viewModelScope.launch {
            repository.togglePostLike(postId)
        }
    }

    fun toggleDislike(postId: String) {
        viewModelScope.launch {
            repository.togglePostDislike(postId)
        }
    }

    fun addComment(postId: String, commentText: String) {
        viewModelScope.launch {
            repository.addPostComment(postId, commentText)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
        }
    }

    fun editPostDescription(postId: String, newDescription: String) {
        viewModelScope.launch {
            repository.updatePostDescription(postId, newDescription)
        }
    }
}