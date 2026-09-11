package com.example.nutriwise.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// NutriWise Dark Palette (Reddit-inspired Charcoal & Emerald)
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF18181C),
    surfaceVariant = Color(0xFF24242A),
    onBackground = Color(0xFFEEEEEE),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFB0B0B8)
)

// NutriWise Light Palette (Clean Organic Green & Off-White)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5E9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFF00796B),
    background = Color(0xFFF8F9FA),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F3F4),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF202124),
    onSurfaceVariant = Color(0xFF5F6368)
)

@Composable
fun NutriWiseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}