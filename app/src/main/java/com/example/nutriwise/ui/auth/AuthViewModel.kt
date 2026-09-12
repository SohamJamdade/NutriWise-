package com.example.nutriwise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriwise.data.FirebaseRepository
import com.example.nutriwise.domain.UserProfile
import com.google.firebase.auth.AuthCredential
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.nutriwise.ui.auth.AuthViewModel

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    object Authenticated : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel : ViewModel() {

    private val repository = FirebaseRepository()

    val currentUser: com.google.firebase.auth.FirebaseUser?
        get() = repository.currentUser

    private val _uiState = MutableStateFlow<AuthUiState>(
        if (repository.isUserLoggedIn) AuthUiState.Authenticated else AuthUiState.Idle
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    init {
        if (repository.isUserLoggedIn) {
            loadUserProfile()
        }
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            _userProfile.value = repository.fetchCurrentUserProfile()
        }
    }

    fun signInWithGoogle(credential: AuthCredential) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.signInWithGoogleCredential(credential)
            if (result.isSuccess) {
                _userProfile.value = result.getOrNull()
                _uiState.value = AuthUiState.Authenticated
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Google Sign In Failed")
            }
        }
    }

    fun login(email: String, pass: String) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = repository.login(email, pass)
            if (result.isSuccess) {
                loadUserProfile()
                _uiState.value = AuthUiState.Authenticated
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUpWithHealthToggles(
        email: String,
        pass: String,
        username: String,
        hasDiabetes: Boolean,
        hasHypertension: Boolean,
        hasHighCholesterol: Boolean,
        allergens: List<String>,
        otherConditions: List<String>
    ) {
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val profile = UserProfile(
                username = username,
                email = email,
                healthConditions = buildList {
                    if (hasDiabetes) add("Diabetes / Blood Sugar")
                    if (hasHypertension) add("Hypertension (High BP)")
                    if (hasHighCholesterol) add("High Cholesterol")
                    addAll(allergens)
                    addAll(otherConditions)
                }
            )
            val result = repository.signUp(email, pass, username, profile)
            if (result.isSuccess) {
                _userProfile.value = profile
                _uiState.value = AuthUiState.Authenticated
            } else {
                _uiState.value = AuthUiState.Error(result.exceptionOrNull()?.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun saveProfileUpdates(updatedProfile: UserProfile) {
        viewModelScope.launch {
            try {
                val result = repository.saveUserProfile(updatedProfile)
                if (result.isSuccess) {
                    _userProfile.value = updatedProfile
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun completeSurvey(conditions: List<String>) {
        viewModelScope.launch {
            val result = repository.completeHealthSurvey(conditions)
            if (result.isSuccess) {
                _userProfile.value = _userProfile.value?.copy(
                    healthConditions = conditions,
                    isOnboardingCompleted = true
                )
            }
        }
    }

    fun updateConditionsFromProfile(newConditions: List<String>) {
        viewModelScope.launch {
            val result = repository.updateUserHealthConditions(newConditions)
            if (result.isSuccess) {
                _userProfile.value = _userProfile.value?.copy(
                    healthConditions = newConditions
                )
            }
        }
    }

    fun logout() {
        repository.logout()
        _userProfile.value = null
        _uiState.value = AuthUiState.Idle
    }
}