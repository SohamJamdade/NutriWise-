package com.example.nutriwise.profile

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.nutriwise.domain.ReviewPost
import com.example.nutriwise.ui.auth.AuthViewModel
import com.example.nutriwise.ui.feed.FeedViewModel
import com.example.nutriwise.ui.profile.NutriWiseProfileScreen as UiNutriWiseProfileScreen

@Deprecated(
    message = "Use com.example.nutriwise.ui.profile.NutriWiseProfileScreen instead.",
    replaceWith = ReplaceWith("NutriWiseProfileScreen(authViewModel, feedViewModel, userPosts, isDarkMode, onToggleTheme, modifier)", "com.example.nutriwise.ui.profile.NutriWiseProfileScreen")
)
@Composable
fun NutriWiseProfileScreen(
    authViewModel: AuthViewModel,
    feedViewModel: FeedViewModel,
    userPosts: List<ReviewPost>,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    UiNutriWiseProfileScreen(
        authViewModel = authViewModel,
        feedViewModel = feedViewModel,
        userPosts = userPosts,
        isDarkMode = isDarkMode,
        onToggleTheme = onToggleTheme,
        modifier = modifier
    )
}
